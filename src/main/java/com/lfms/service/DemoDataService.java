package com.lfms.service;

import com.lfms.dao.UserDAO;
import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;

import java.time.LocalDate;

/**
 * Seeds a small set of realistic demo data (students, lost/found items that match, and a
 * pending claim) so the application can be explored immediately. Idempotent: it does
 * nothing if the demo data is already present. Goes through the real services so the
 * matching engine and audit log run exactly as in normal use.
 */
public class DemoDataService {

    public static final String DEMO_PASSWORD = "Password1";
    private static final String MARKER_EMAIL = "kofi@university.edu";

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();
    private final ItemService itemService = new ItemService();
    private final ClaimService claimService = new ClaimService();

    /** @return true if demo data was seeded, false if it was already present. */
    public boolean seedIfEmpty() {
        if (userDAO.findByEmail(MARKER_EMAIL) != null) {
            return false;
        }

        authService.register("Kofi Mensah", "20480001", MARKER_EMAIL, DEMO_PASSWORD, "0240000001");
        authService.register("Ama Owusu", "20480002", "ama@university.edu", DEMO_PASSWORD, "0240000002");
        authService.register("Yaw Boateng", "20480003", "yaw@university.edu", DEMO_PASSWORD, "0240000003");

        User kofi = userDAO.findByEmail(MARKER_EMAIL);
        User ama = userDAO.findByEmail("ama@university.edu");
        User yaw = userDAO.findByEmail("yaw@university.edu");

        report(kofi, Item.TYPE_LOST, "Black HP laptop", "Electronics",
                "Black HP Pavilion laptop with a small dent on the lid and a blue university sticker.",
                "Main Library", 2);
        int foundLaptop = report(ama, Item.TYPE_FOUND, "HP laptop", "Electronics",
                "Found a black HP laptop near the main library entrance.", "Main Library", 1);

        report(yaw, Item.TYPE_LOST, "Blue water bottle", "Accessories",
                "Blue stainless-steel water bottle, 750ml, with a dent near the base.", "SWL Block", 3);
        report(ama, Item.TYPE_FOUND, "Blue water bottle", "Accessories",
                "Blue metal water bottle left in lecture hall B.", "SWL Block", 1);

        report(kofi, Item.TYPE_FOUND, "Set of keys", "Keys",
                "A set of about five keys on a red lanyard.", "Cafeteria", 0);
        report(yaw, Item.TYPE_LOST, "Student ID card", "ID/Cards",
                "Student ID card, lost somewhere near the main bus stop.", "Bus Stop", 1);

        Claim claim = new Claim();
        claim.setItemId(foundLaptop);
        claim.setClaimantId(kofi.getUserId());
        claim.setFeaturesDesc("It has a dent on the lid and a blue university sticker; my initials KM are inside the battery cover.");
        claim.setProofDesc("I have the original purchase receipt from the campus store and photos of the laptop on my phone.");
        claim.setStatus(Claim.STATUS_PENDING);
        claimService.submitClaim(claim);

        return true;
    }

    private int report(User user, String type, String name, String category, String description,
                       String location, int daysAgo) {
        Item item = new Item();
        item.setUserId(user.getUserId());
        item.setType(type);
        item.setName(name);
        item.setCategory(category);
        item.setDescription(description);
        item.setLocation(location);
        item.setStatus(Item.STATUS_OPEN);
        item.setDateReported(LocalDate.now().minusDays(daysAgo).toString());
        return itemService.reportItem(item, null);
    }
}
