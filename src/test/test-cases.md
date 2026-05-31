# Lost & Found Management System — Functional Test Cases

These are manual functional test cases covering the main modules of the application.
Run them against a fresh database (the app seeds the default admin automatically).
Fill in the **Status** column with `Pass`, `Fail` or `Not Tested` during testing.

**Default admin:** `admin@lfms.edu` / `Admin@1234`

| TC# | Module | Test Description | Input | Expected Result | Status |
|-----|--------|------------------|-------|-----------------|--------|
| TC01 | Registration | Register a new student with valid, unique details | Name: `Ama Owusu`, Index: `10551234`, Email: `ama@uni.edu`, Password: `Pass1234`, Confirm: `Pass1234` | Account created; redirected to Login with "Account created successfully" message | Not Tested |
| TC02 | Registration | Reject duplicate email | Email: `admin@lfms.edu` (already exists), other fields valid | Inline error under Email: "An account with this email already exists."; no account created | Not Tested |
| TC03 | Registration | Reject duplicate index number | Index: `ADMIN001` (already exists), other fields valid | Inline error under Index: "This index / ID number is already registered." | Not Tested |
| TC04 | Registration | Reject mismatched passwords | Password: `Pass1234`, Confirm: `Pass9999` | Inline error under Confirm: "Passwords do not match." | Not Tested |
| TC05 | Login | Log in as a valid student | Email + password of a registered student | Redirected to the student Dashboard | Not Tested |
| TC06 | Login | Log in as admin | `admin@lfms.edu` / `Admin@1234` | Redirected to the Admin Dashboard | Not Tested |
| TC07 | Login | Reject wrong password | Valid email, wrong password | Inline error: "Invalid email or password." | Not Tested |
| TC08 | Report Lost | Submit a valid lost report | Name, Category, Description filled | Item saved; alert "Lost item reported! N possible matches found."; back to Dashboard | Not Tested |
| TC09 | Report Lost | Reject missing required fields | Leave Name / Category / Description empty | Inline red errors under each empty field; nothing saved | Not Tested |
| TC10 | Report Lost | Attach an image | Choose a `.png`/`.jpg` file | Thumbnail preview shows; on submit the image is copied into `data/images/` | Not Tested |
| TC11 | Report Found | Submit a valid found report | Name, Category, Description, Location filled + image chosen | Item saved; success alert; back to Dashboard | Not Tested |
| TC12 | Report Found | Require location | Location left empty | Inline error: "Location is required for found items." | Not Tested |
| TC13 | Report Found | Require image | No image selected | Inline error: "An image is required for found items." | Not Tested |
| TC14 | Browse / Search | Keyword search | Search box: `laptop` | Table shows only items whose name/description contains "laptop" | Not Tested |
| TC15 | Browse / Search | Category filter | Category: `Electronics` | Table shows only Electronics items | Not Tested |
| TC16 | Browse / Search | No matching results | Search box: `zzzzzz` | Empty table with placeholder "No items found matching your search." | Not Tested |
| TC17 | Submit Claim | Submit a valid claim | Features ≥ 30 chars, Proof ≥ 30 chars on an open FOUND item | Claim saved; item status → CLAIM_PENDING; success alert; back to Browse | Not Tested |
| TC18 | Submit Claim | Block duplicate claim | Open Claim form for an item the user already has a pending claim on | Form hidden; warning "You already submitted a claim for this item." | Not Tested |
| TC19 | Submit Claim | Enforce minimum length | Features: `mine` (4 chars) | Inline error requiring at least 30 characters; not submitted | Not Tested |
| TC20 | Admin Claims | Approve a claim | Select a pending claim, click Approve | Claim → APPROVED, item → APPROVED, other pending claims on that item auto-rejected | Not Tested |
| TC21 | Admin Claims | Reject a claim with reason | Enter reason, click Reject | Claim → REJECTED with the reason saved; item reverts to OPEN (if no other pending claims) | Not Tested |
| TC22 | Admin Reports | Delete a report | Click Delete, enter a reason, confirm | Item and its claims/matches removed; action recorded in the audit log | Not Tested |
| TC23 | Admin Users | Deactivate a user | Click Deactivate on an active user | Status badge → Inactive; that user can no longer log in | Not Tested |
| TC24 | Matching Engine | Match on same category + keywords | Report LOST "Black HP laptop" (Electronics, Main Library), then FOUND "HP laptop" (Electronics, Main Library) | A match is created (score ≥ 4) and appears under "Suggested Matches" | Not Tested |
| TC25 | Matching Engine | No match below threshold | Report LOST "Red umbrella" (Accessories) and FOUND "Calculator" (Electronics), nothing in common | Score < 4, so no match is created | Not Tested |

---

### Test Environment

- **OS:** Windows 11
- **JDK:** 17+ (developed and verified on JDK 26)
- **Build:** Apache Maven 3.6+
- **Run command:** `mvn javafx:run`
