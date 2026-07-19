# Queda UX Foundation

## Design Principles

Queda's visual language is built on the following principles:

- **Minimal & Premium**: Focus on content and clarity. Avoid unnecessary decoration or complex gradients.
- **Calm & distinctive**: Use a restrained color palette with a strong primary accent (Deep Teal).
- **Fast & Coherent**: Consistent use of tokens and components ensures a predictable and snappy experience.
- **Accessible by Default**: High contrast, generous touch targets (48dp+), and support for large fonts.

## Tokens

### Color
- **Primary**: Deep Teal (#004D40) - Used for primary actions and key accents.
- **Background**: Warm/Neutral Light (#FAF9F6) - Provides a calm surface for content.
- **Text**: Near-black (#1A1C1E) for primary, Restrained Grey (#44474E) for secondary.
- **Error**: Red (#BA1A1A) - Strictly reserved for errors and destructive actions.

### Spacing
Scale: 4, 8, 12, 16, 20, 24, 32, 40 dp.
Tokens: `None`, `Tiny`, `Small`, `SmallMedium`, `Medium`, `MediumLarge`, `Large`, `ExtraLarge`, `Huge`.

### Typography
- **Headline**: Bold, SansSerif.
- **Title**: Semi-bold, for hierarchy within screens.
- **Body**: Normal weight, 14-16sp, optimized for readability.

## Component Ownership

- **core:designsystem**: Generic visual primitives, theme tokens, and stateless reusable components (Buttons, Fields, Chips, Scaffolds, Bottom Action Bar).
- **feature modules**: Domain-specific components (e.g., `InventoryItemRow`) and UI models.

## Reusable Component Catalog

- `QuedaScaffold`: Standard screen layout container with Material 3 content insets.
- `QuedaTopAppBar`: Consistent header with title and actions.
- `QuedaBottomActionBar`: Full-width container for primary actions, supporting navigation-bar and IME protection.
- `QuedaPrimaryButton`: Main call-to-action with loading state support.
- `QuedaSecondaryButton`: Outlined button for secondary actions.
- `QuedaDestructiveButton`: Red outlined button for destructive actions.
- `QuedaTextField`: Standard text input with error state and supporting text.
- `QuedaNumericField`: Specialized text field for decimal input.
- `QuedaEmptyState`: Full-screen placeholder for empty views.
- `QuedaLoadingState`: Centered loading indicator with localized accessibility description.
- `QuedaErrorState`: Centered error message with retry action.
- `QuedaModalBottomSheet`: Standard container for bottom sheets.

## Accessibility Contract

- **Touch Targets**: Minimum 48x48 dp for all interactive elements.
- **Contrast**: High contrast ratios for all text and icons.
- **Semantics**: Meaningful accessibility labels, error state announcements, and selectable roles for lists.
- **Responsiveness**: Layouts must support large font sizes and various screen sizes (API 28+).

## System Contracts

### System Bar Contract
Scaffolds and full-screen components must respect system bars using `WindowInsets`.

### IME Contract
Input screens must ensure primary actions remain visible above the keyboard using `imePadding` and `QuedaBottomActionBar`.

### Unit Selector Semantics
- Closed state: `Role.Button`, announces current unit.
- Sheet options: `selectable` with `Role.RadioButton`.

## Current Screens

### Inventory Screen
- Displays the list of food items with name and quantity.
- Summary header showing total count (singular/plural support).
- Fixed bottom action bar for adding new items.
- Polished empty, loading, and error states.

### Add Exact Item Screen
- Single-column form for manual entry.
- Immediate focus on Name field.
- Unit selection via Modal Bottom Sheet with selectable semantics.
- Inline validation with human-readable error messages and accessibility announcements.
- Primary Save action, protected by IME padding.

## Tests Executed

- **Architecture**: Verified no feature leaks into `core:designsystem` and no `TODO` placeholders.
- **Design System**: Verified touch targets, loading states, error semantics, and generic component behavior.
- **Inventory**: Verified loading descriptions, summary logic, quantity formats, and scrolling behavior.
- **Add Item**: Verified focus management, IME actions, unit selection, and saving states.
- **App Shell**: Verified end-to-end navigation scenario.

## Roadmap Activation Rules

- **Item Row Navigation**: Begins when Edit/Delete functionality is added. Currently rows are not clickable.
- **Consume UI**: Begins with Consume/Correct features.
- **Scanner UI**: Begins with Barcode support.
- **Location Metadata & Filters**: Begins with Locations support.
- **Status Chips**: Begins with Expiry/Opened status support.
- **Today/Shopping Navigation**: Begins when those features are activated.
- **Bottom Navigation**: Shown only when at least two main destinations (e.g., Inventory, Today, Shopping) exist.
