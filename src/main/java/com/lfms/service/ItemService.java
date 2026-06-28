package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.dao.ClaimDAO;
import com.lfms.dao.ItemDAO;
import com.lfms.dao.MatchDAO;
import com.lfms.model.Item;
import com.lfms.model.LabelCount;
import com.lfms.model.Match;
import com.lfms.model.MatchBreakdown;
import com.lfms.util.ImageUtil;
import com.lfms.util.QrCodeUtil;

import java.io.File;
import java.util.List;

/**
 * Item reporting, searching, editing and deletion. Also orchestrates the matching engine
 * when a new item is reported.
 */
public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();
    private final MatchDAO matchDAO = new MatchDAO();
    private final ClaimDAO claimDAO = new ClaimDAO();
    private final AuditLogDAO auditDAO = new AuditLogDAO();
    private final MatchingEngine matchingEngine = new MatchingEngine();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Creates a new item, copying the optional image into storage, then runs the matching
     * engine (which persists any matches). Returns the new item's id.
     */
    public int reportItem(Item item, File imageFile) {
        if (imageFile != null) {
            item.setImagePath(ImageUtil.copyImageToStorage(imageFile));
        }
        if (item.getStatus() == null) {
            item.setStatus(Item.STATUS_OPEN);
        }
        int id = itemDAO.create(item);
        item.setItemId(id);

        auditDAO.log(item.getUserId(), "REPORT_ITEM", "ITEM", id,
                item.getType() + " item reported: " + item.getName());

        // Run the matching engine; matches are persisted inside the engine.
        List<Match> matches = matchingEngine.findMatches(item);

        // Generate a QR code for found items so it can be displayed/saved later.
        if (item.isFound()) {
            QrCodeUtil.generateForItem(id);
        }

        // Notify the owners of any existing items this new report matched.
        notifyCounterpartOwners(item, matches);
        return id;
    }

    /**
     * For each match the engine produced, notify the owner of the <em>other</em> (existing) item
     * that a new report may match theirs. The reporter is never notified about their own report.
     */
    private void notifyCounterpartOwners(Item newItem, List<Match> matches) {
        for (Match match : matches) {
            int counterpartId = match.getLostItemId() == newItem.getItemId()
                    ? match.getFoundItemId() : match.getLostItemId();
            Item counterpart = itemDAO.findById(counterpartId);
            if (counterpart == null || counterpart.getUserId() == newItem.getUserId()) {
                continue;
            }
            notificationService.notify(counterpart.getUserId(),
                    "Possible match for your report \"" + counterpart.getName() + "\": a new "
                            + newItem.getType().toLowerCase() + " item \"" + newItem.getName()
                            + "\" was just reported.");
        }
    }

    public List<Item> search(String keyword, String type, String category, String location,
                             String dateFrom, String dateTo) {
        return itemDAO.search(keyword, type, category, location, dateFrom, dateTo);
    }

    public Item findById(int itemId) {
        return itemDAO.findById(itemId);
    }

    public List<Item> findAll() {
        return itemDAO.findAll();
    }

    public List<Item> findByUser(int userId) {
        return itemDAO.findByUser(userId);
    }

    public boolean updateItem(Item item) {
        boolean ok = itemDAO.update(item);
        if (ok) {
            auditDAO.log(item.getUserId(), "EDIT_ITEM", "ITEM", item.getItemId(),
                    "Item edited: " + item.getName());
        }
        return ok;
    }

    public boolean updateStatus(int itemId, String status, int actorId) {
        boolean ok = itemDAO.updateStatus(itemId, status);
        if (ok) {
            auditDAO.log(actorId, "CHANGE_STATUS", "ITEM", itemId, "Status changed to " + status);
        }
        return ok;
    }

    public boolean deleteItem(int itemId, int actorId) {
        return deleteItem(itemId, actorId, null);
    }

    /**
     * Deletes an item along with its dependent matches and claims (children first to
     * respect foreign keys), then records the action with an optional reason.
     */
    public boolean deleteItem(int itemId, int actorId, String reason) {
        Item item = itemDAO.findById(itemId);
        matchDAO.deleteByItem(itemId);
        claimDAO.deleteByItem(itemId);
        boolean ok = itemDAO.delete(itemId);
        if (ok) {
            String note = "Item deleted" + (item != null ? ": " + item.getName() : "");
            if (reason != null && !reason.isBlank()) {
                note += " — reason: " + reason.trim();
            }
            auditDAO.log(actorId, "DELETE_ITEM", "ITEM", itemId, note);

            // If an administrator removed someone else's report, let the owner know.
            if (item != null && item.getUserId() != actorId) {
                notificationService.notify(item.getUserId(),
                        "Your report \"" + item.getName() + "\" was removed by an administrator"
                                + (reason != null && !reason.isBlank() ? " — reason: " + reason.trim() : "")
                                + ".");
            }
        }
        return ok;
    }

    // ---- counts & matches (for dashboards) ----

    public int countByType(String type) {
        return itemDAO.countByType(type);
    }

    public int countByStatus(String status) {
        return itemDAO.countByStatus(status);
    }

    public int countOpenByType(String type) {
        return itemDAO.findOpenByType(type).size();
    }

    public int countAll() {
        return itemDAO.countAll();
    }

    /** Items considered recovered: those whose claim was approved or that were resolved. */
    public int countRecovered() {
        return itemDAO.countByStatus(Item.STATUS_RESOLVED) + itemDAO.countByStatus(Item.STATUS_APPROVED);
    }

    /**
     * Recovery rate as a percentage: {@code (recovered / total) * 100}. Returns 0 when there are
     * no items. Callers round for display.
     */
    public double recoveryRate() {
        int total = countAll();
        return total == 0 ? 0.0 : (countRecovered() * 100.0) / total;
    }

    /** The most-reported lost category and its count (or {@code null} when nothing is lost). */
    public LabelCount topLostCategory() {
        return itemDAO.topLostCategory();
    }

    /** Top-{@code limit} locations where items were reported lost, ranked by count. */
    public List<LabelCount> topLostLocations(int limit) {
        return itemDAO.topLostLocations(limit);
    }

    /** Matches for a single item, looked up by the side that item sits on. */
    public List<Match> getMatchesForItem(Item item) {
        if (item == null) {
            return List.of();
        }
        return item.isLost()
                ? matchDAO.findByLostItem(item.getItemId())
                : matchDAO.findByFoundItem(item.getItemId());
    }

    /** All matches involving any item belonging to the user (for the dashboard), each with its
     *  explanation attached so the UI can show why it matched without recomputing inline. */
    public List<Match> getMatchesForUser(int userId) {
        List<Match> matches = matchDAO.findByUser(userId);
        for (Match match : matches) {
            match.setBreakdown(explainMatch(match));
        }
        return matches;
    }

    /** Regenerates the structured breakdown for a persisted match from its two items. */
    public MatchBreakdown explainMatch(Match match) {
        Item lost = itemDAO.findById(match.getLostItemId());
        Item found = itemDAO.findById(match.getFoundItemId());
        return matchingEngine.explain(lost, found);
    }
}
