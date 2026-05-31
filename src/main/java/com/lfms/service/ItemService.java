package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.dao.ClaimDAO;
import com.lfms.dao.ItemDAO;
import com.lfms.dao.MatchDAO;
import com.lfms.model.Item;
import com.lfms.model.Match;
import com.lfms.util.ImageUtil;

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
        matchingEngine.findMatches(item);
        return id;
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

    /** Matches for a single item, looked up by the side that item sits on. */
    public List<Match> getMatchesForItem(Item item) {
        if (item == null) {
            return List.of();
        }
        return item.isLost()
                ? matchDAO.findByLostItem(item.getItemId())
                : matchDAO.findByFoundItem(item.getItemId());
    }

    /** All matches involving any item belonging to the user (for the dashboard). */
    public List<Match> getMatchesForUser(int userId) {
        return matchDAO.findByUser(userId);
    }
}
