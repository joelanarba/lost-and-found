package com.lfms.service;

import com.lfms.dao.UserDAO;
import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.util.SeedImageFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a rich, realistic demo dataset for the University of Cape Coast (UCC): a set of
 * students, ~30 lost/found items located at real UCC landmarks (each with a generated
 * category image), several deliberately-matching lost&#8596;found pairs, and a pending
 * claim. Idempotent — does nothing if the demo data is already present. Everything goes
 * through the real services so the matching engine and audit log run as in normal use.
 */
public class DemoDataService {

    public static final String DEMO_PASSWORD = "Password1";
    public static final String DEMO_EMAIL    = "kofi.mensah@stu.ucc.edu.gh";
    private static final String MARKER_EMAIL = DEMO_EMAIL;

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();
    private final ItemService itemService = new ItemService();
    private final ClaimService claimService = new ClaimService();

    /** @return true if demo data was seeded, false if it was already present. */
    public boolean seedIfEmpty() {
        if (userDAO.findByEmail(MARKER_EMAIL) != null) {
            return false;
        }

        // --- Students (UCC, @stu.ucc.edu.gh) ---
        register("Kofi Mensah",    "SB/ITC/21/0001", MARKER_EMAIL,                    "0244000001");
        register("Ama Owusu",      "FA/ENG/21/0042", "ama.owusu@stu.ucc.edu.gh",      "0244000002");
        register("Yaw Boateng",    "SP/PHY/20/0113", "yaw.boateng@stu.ucc.edu.gh",    "0244000003");
        register("Akosua Agyeman", "FE/EDU/22/0207", "akosua.agyeman@stu.ucc.edu.gh", "0244000004");
        register("Kwame Appiah",   "SB/ACC/21/0388", "kwame.appiah@stu.ucc.edu.gh",   "0244000005");
        register("Efua Sarpong",   "SM/NUR/20/0076", "efua.sarpong@stu.ucc.edu.gh",   "0244000006");
        register("Kwesi Nkrumah",  "SP/CHE/22/0511", "kwesi.nkrumah@stu.ucc.edu.gh",  "0244000007");
        register("Abena Asante",   "FA/HIS/21/0299", "abena.asante@stu.ucc.edu.gh",   "0244000008");

        User kofi   = user(MARKER_EMAIL);
        User ama    = user("ama.owusu@stu.ucc.edu.gh");
        User yaw    = user("yaw.boateng@stu.ucc.edu.gh");
        User akosua = user("akosua.agyeman@stu.ucc.edu.gh");
        User kwame  = user("kwame.appiah@stu.ucc.edu.gh");
        User efua   = user("efua.sarpong@stu.ucc.edu.gh");
        User kwesi  = user("kwesi.nkrumah@stu.ucc.edu.gh");
        User abena  = user("abena.asante@stu.ucc.edu.gh");

        // --- Matching lost & found pairs (drive the suggestion engine) ---
        report(kofi, Item.TYPE_LOST, "Black HP Pavilion laptop", "Electronics",
                "Black HP Pavilion 15 laptop with a small dent on the lid and a blue UCC sticker.",
                "Sam Jonah Library", 2);
        int foundLaptop = report(ama, Item.TYPE_FOUND, "HP laptop", "Electronics",
                "Found a black HP laptop near the Sam Jonah Library entrance.", "Sam Jonah Library", 1);

        report(kofi, Item.TYPE_LOST, "Blue water bottle", "Accessories",
                "Blue stainless-steel water bottle, 750ml, with a dent near the base.",
                "Science Lecture Theatre", 3);
        int foundWaterBottle = report(yaw, Item.TYPE_FOUND, "Blue water bottle", "Accessories",
                "Blue metal water bottle left in the Science Lecture Theatre.", "Science Lecture Theatre", 2);

        report(kofi, Item.TYPE_LOST, "Student ID card", "ID/Cards",
                "UCC student ID card in the name of Kofi Mensah.", "Casely-Hayford Hall", 4);
        int foundIdCard = report(akosua, Item.TYPE_FOUND, "UCC student ID card", "ID/Cards",
                "Found a UCC student ID card near the Casely-Hayford Hall porter's lodge.",
                "Casely-Hayford Hall", 3);

        report(kwame, Item.TYPE_LOST, "Set of keys", "Keys",
                "A set of about five keys on a red lanyard.", "Science Market", 2);
        int foundKeys = report(kofi, Item.TYPE_FOUND, "Bunch of keys", "Keys",
                "Bunch of keys on a red lanyard found at the Science Market.", "Science Market", 1);

        report(efua, Item.TYPE_LOST, "Black North Face backpack", "Bags",
                "Black North Face backpack with a maths textbook inside.", "New Lecture Theatre", 3);
        int foundBackpack = report(kwesi, Item.TYPE_FOUND, "Black backpack", "Bags",
                "Black backpack found under a seat in the New Lecture Theatre.", "New Lecture Theatre", 2);

        // --- Standalone items across categories & UCC locations ---
        report(ama,    Item.TYPE_LOST,  "Casio fx-991 calculator", "Books & Stationery",
                "Casio fx-991EX scientific calculator, slightly scratched screen.", "School of Business", 5);
        report(yaw,    Item.TYPE_FOUND, "Black umbrella", "Other",
                "Black foldable umbrella left at the Apewosika bus stop.", "Apewosika", 1);
        report(akosua, Item.TYPE_FOUND, "Prescription glasses", "Accessories",
                "Prescription glasses in a brown case.", "Sam Jonah Library", 2);
        report(kwame,  Item.TYPE_LOST,  "Samsung phone charger", "Electronics",
                "White Samsung fast charger with USB-C cable.", "ICT Centre", 3);
        report(efua,   Item.TYPE_FOUND, "Silver wristwatch", "Accessories",
                "Silver wristwatch found in the Adehye Hall washroom.", "Adehye Hall", 4);
        report(kwesi,  Item.TYPE_LOST,  "Blue lab coat", "Clothing",
                "Blue lab coat with a name tag reading E. Mensah.", "Science Lecture Theatre", 6);
        report(abena,  Item.TYPE_FOUND, "Grey UCC hoodie", "Clothing",
                "Grey University of Cape Coast hoodie left in a lecture.", "Faculty of Arts", 2);
        report(abena,  Item.TYPE_LOST,  "Wireless earbuds", "Electronics",
                "White wireless earbuds in a small charging case.", "Main Auditorium", 1);
        report(kofi,   Item.TYPE_FOUND, "Chemistry notebook", "Books & Stationery",
                "A4 hardcover notebook full of chemistry notes.", "Sasakawa Lecture Theatre", 3);
        report(ama,    Item.TYPE_FOUND, "House keys", "Keys",
                "Two keys on a keyring with a small torch.", "Oguaa Hall", 5);
        report(yaw,    Item.TYPE_LOST,  "Oraimo power bank", "Electronics",
                "20000mAh black Oraimo power bank.", "ICT Centre", 2);
        report(akosua, Item.TYPE_LOST,  "Hall meal card", "ID/Cards",
                "Adehye Hall meal card.", "Adehye Hall", 4);
        report(kwame,  Item.TYPE_FOUND, "SanDisk flash drive", "Electronics",
                "32GB SanDisk flash drive, no label.", "ICT Centre", 1);
        report(efua,   Item.TYPE_FOUND, "Economics textbook", "Books & Stationery",
                "Introduction to Economics textbook, second edition.", "School of Business", 6);
        report(kwesi,  Item.TYPE_FOUND, "Beaded bracelet", "Accessories",
                "Beaded bracelet in red and gold.", "Main Auditorium", 3);
        report(abena,  Item.TYPE_FOUND, "Brown leather wallet", "Other",
                "Brown leather wallet, no cash inside, only an ID slot.", "Valco Hall", 2);
        report(kofi,   Item.TYPE_LOST,  "Ray-Ban sunglasses", "Accessories",
                "Black Ray-Ban sunglasses in a hard case.", "UCC Sports Stadium", 5);
        report(ama,    Item.TYPE_LOST,  "Dell laptop charger", "Electronics",
                "Dell 65W laptop charger.", "Sam Jonah Library", 3);
        report(yaw,    Item.TYPE_FOUND, "Navy baseball cap", "Clothing",
                "Navy blue baseball cap.", "UCC Sports Stadium", 1);
        report(kwame,  Item.TYPE_LOST,  "Library borrower card", "ID/Cards",
                "Sam Jonah Library borrower card.", "Sam Jonah Library", 7);
        report(efua,   Item.TYPE_FOUND, "Yellow scarf", "Clothing",
                "Yellow patterned scarf.", "Atlantic Hall", 2);
        report(kwesi,  Item.TYPE_LOST,  "Graphic calculator", "Books & Stationery",
                "Texas Instruments graphic calculator.", "Faculty of Arts", 3);
        report(abena,  Item.TYPE_FOUND, "Tecno phone", "Electronics",
                "Tecno phone found at the University Hospital waiting area.", "University Hospital", 2);

        // --- A pending claim on the found laptop (gives admins something to review) ---
        Claim claim = new Claim();
        claim.setItemId(foundLaptop);
        claim.setClaimantId(kofi.getUserId());
        claim.setFeaturesDesc("It has a dent on the lid and a blue UCC sticker; my initials KM are inside the battery cover.");
        claim.setProofDesc("I have the original purchase receipt from the campus store and photos of the laptop on my phone.");
        claim.setStatus(Claim.STATUS_PENDING);
        claimService.submitClaim(claim);

        // --- Completed lifecycles so the recovery rate, item timelines and notifications have
        //     real data to show. Goes through the real services (audit + notifications fire). ---
        User admin = userDAO.findByEmail("admin@lfms.edu");
        int adminId = admin != null ? admin.getUserId() : -1;

        // Kofi's ID card: claimed, approved by an admin, then handed over (fully resolved).
        submitClaim(foundIdCard, kofi,
                "It's my UCC ID — name Kofi Mensah, index SB/ITC/21/0001.",
                "I can quote the exact index number and describe the photo on the card.");
        approveLatestClaim(foundIdCard, adminId);
        itemService.updateStatus(foundIdCard, Item.STATUS_RESOLVED, adminId);

        // Efua's backpack: claimed and approved, now awaiting collection.
        submitClaim(foundBackpack, efua,
                "Black North Face bag with my maths textbook and a blue pencil case inside.",
                "The textbook has my name written on the first page.");
        approveLatestClaim(foundBackpack, adminId);

        // Two straightforward handovers returned to their owners.
        itemService.updateStatus(foundWaterBottle, Item.STATUS_RESOLVED, adminId);
        itemService.updateStatus(foundKeys, Item.STATUS_RESOLVED, adminId);

        return true;
    }

    private void submitClaim(int itemId, User claimant, String features, String proof) {
        Claim claim = new Claim();
        claim.setItemId(itemId);
        claim.setClaimantId(claimant.getUserId());
        claim.setFeaturesDesc(features);
        claim.setProofDesc(proof);
        claim.setStatus(Claim.STATUS_PENDING);
        claimService.submitClaim(claim);
    }

    private void approveLatestClaim(int itemId, int adminId) {
        List<Claim> claims = claimService.findByItem(itemId);
        if (!claims.isEmpty()) {
            claimService.approveClaim(claims.get(0).getClaimId(), adminId);
        }
    }

    private void register(String name, String indexNo, String email, String phone) {
        authService.register(name, indexNo, email, DEMO_PASSWORD, phone);
    }

    private User user(String email) {
        return userDAO.findByEmail(email);
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
        File image = SeedImageFactory.generate(category, type, name);
        return itemService.reportItem(item, image);
    }
}
