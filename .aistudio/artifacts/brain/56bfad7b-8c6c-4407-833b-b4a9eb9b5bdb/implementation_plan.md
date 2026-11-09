# Deep Black Expressive Dock, Stable 55% Bottom Sheet, XL Floating Nav, Rich Item Fields & Dual QR/Barcode Studio

Design and implementation plan to remove the home screen quick-add row, apply high-contrast deep black container styling to the search bar and floating pill navigation dock, adjust the Add Item bottom sheet to a stable half-screen height (55%), expand the floating navigation bar to XL dimensions, introduce brand, price, priority, and favorite attributes to grocery items, and upgrade the scanner into a 3-way studio supporting 1D barcodes, 2D QR codes, on-device QR generation, and barcode-to-QR conversion.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification:
> - **Add Item Sheet Height**: Set to a **stable half-height (55% screen height)** that remains grounded at the bottom with smooth keyboard adjustment, replacing the previous 90% full-bleed height.
> - **New Add Item Attributes**: Added **Price ($)**, **Priority Urgency (Low / Normal / Urgent)**, **Brand Name**, and **Favorite (Starred) Toggle**.
> - **QR & Barcode Scanner Studio**: Features a **3-way segmented toggle** (**Scan Barcode**, **Scan QR Code**, **Generate QR / Convert**), supporting 1D retail UPC/EAN scanning, 2D QR checklist importing, and generating on-screen QR codes from barcodes or item details.
> - **Deep Black Styling**: Both the top search container and the floating navigation pill dock are filled with **deep jet black (`#0B0B0E`)**, paired with crisp high-contrast text and icons.
> - **XL Floating Navbar**: Scaled up to **62dp height** with **26dp icons** and expanded pill touch targets.
> - **Home Screen Cleanup**: Removed the "Quick Add" chip row from `GroceriesScreen` to maximize checklist viewing area.

---

### 1. Overview & Core Concept

- **What It Does**:
  - Delivers a striking visual contrast by framing the tinted container background with deep black tactile navigation and search elements.
  - Stabilizes item entry within a comfortable half-screen (55%) bottom sheet that avoids covering the entire screen while editing notes or adding groceries.
  - Enhances grocery tracking with pricing, brands, priority badges, and favorite filters.
  - Provides a unified QR & Barcode Studio enabling users to scan retail barcodes, import QR checklists from friends/family, and convert product barcodes into sharable QR codes.
- **Target Audience / Persona**: Everyday shoppers and meal planners seeking effortless one-handed bottom interactions, budget awareness, and multi-format scanning capabilities.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Streamlined Home Screen**:
   - Clean top search bar filled with sleek deep black (`#0B0B0E`), bold white typography, and instant category filters without distracting quick-add chips.
2. **55% Half-Height Bottom Sheet**:
   - Tapping the XL FAB opens a stable bottom sheet sliding up to exactly 55% of the screen height.
   - Includes drag handle, item name, brand field, price stepper, category chips, priority selector, and favorite star toggle.
3. **XL Floating Pill Dock**:
   - Sits 9dp below the XL FAB with 62dp height, deep black background, smooth spring pill indicators, and prominent icons.
4. **QR & Barcode Scanner Studio**:
   - Full-screen viewfinder with a top segmented switch:
     - **Scan Barcode**: Detects 1D UPC/EAN retail codes and fetches OpenFoodFacts product info.
     - **Scan QR Code**: Reads 2D QR codes containing checklist data or product links with one-tap import.
     - **Generate QR / Convert**: Generates sharp on-screen QR codes for any entered text, barcode number, or grocery item for fast peer-to-peer scanning.

---

### 3. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                              │
│   Filled Container Background + Deep Black Search + XL Floating Dock   │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌─────────────────────────┐           ┌────────────────────────────┐  │
│  │   GroceriesScreen       │           │  Stable 55% Bottom Sheet   │  │
│  │  - Deep Black Search    │           │  - Name & Brand Input      │  │
│  │  - Quick Add Removed    │           │  - Price & Unit Steppers   │  │
│  │  - Price & Priority Badges          │  - Priority & Star Toggle  │  │
│  └───────────┬─────────────┘           └─────────────┬──────────────┘  │
│              │                                       │                 │
│              ▼                                       ▼                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      FreshCartViewModel                          │  │
│  │  - GroceryItem (brand, price, priority, isFavorite)              │  │
│  │  - activeScannerMode (BARCODE, QR_SCAN, QR_GENERATE)             │  │
│  └───────────┬───────────────────────────────────────┬──────────────┘  │
│              │                                       │                 │
│              ▼                                       ▼                 │
│  ┌────────────────────────┐             ┌───────────────────────────┐  │
│  │ BarcodeProductService  │             │   BarcodeQrStudioEngine   │  │
│  │ - OpenFoodFacts API    │             │ - ML Kit 1D/2D Analyzer   │  │
│  │ - 1D UPC/EAN Parsing   │             │ - Local QR Code Matrix Gen│  │
│  │ - Local Fast Dict      │             │ - Barcode to QR Converter │  │
│  └────────────────────────┘             └───────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Schema & Component Modifications:
1. **`GroceryItem.kt`**:
   - Added `brand: String = ""`, `price: Double = 0.0`, `priority: String = "Normal"`, `isFavorite: Boolean = false`.
2. **`Dialogs.kt`**:
   - Updated `AddGroceryDialog` and `AddPantryDialog` to `Modifier.fillMaxWidth().fillMaxHeight(0.55f)` with stable bottom docking, price steppers, brand fields, and priority chips.
3. **`GroceriesScreen.kt`**:
   - Removed quick-add chips row.
   - Updated search bar container to deep black (`Color(0xFF0B0B0E)`).
4. **`ExpressiveFloatingNavBar.kt`**:
   - Upgraded to XL sizing: height 62dp, icon size 26dp, deep black container background (`Color(0xFF0B0B0E)`).
5. **`ItemCards.kt`**:
   - Updated `GroceryItemCard` to show price tag (`$X.XX`), brand subtitle, and priority dot.
6. **`BarcodeScannerDialog.kt` $\rightarrow$ `BarcodeQrStudioDialog.kt`**:
   - Added segmented mode selector: **Scan Barcode**, **Scan QR**, **Generate QR**.
   - Includes on-screen QR rendering with barcode conversion.

---

### 4. Verification Plan

- **Compilation**: Verify build with `compile_applet`.
- **Unit Tests**: Run `gradle :app:testDebugUnitTest` to test new `GroceryItem` attributes, price formatting, and QR encoding logic.
- **Visual Checks**:
  - Confirm deep black contrast on both light and dark themes.
  - Test 55% bottom sheet height and keyboard scrolling behavior.
  - Verify segmented scanner tab switching and QR generation on the streaming emulator.
