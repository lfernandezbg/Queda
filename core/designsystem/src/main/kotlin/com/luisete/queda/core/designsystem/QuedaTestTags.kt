package com.luisete.queda.core.designsystem

object QuedaTestTags {
    const val APP_ROOT = "app_root"
    const val SCREEN_FOUNDATION = "screen_foundation"

    // Onboarding
    const val SCREEN_ONBOARDING = "screen_onboarding"
    const val ONBOARDING_START = "onboarding_start"

    // Bottom Nav
    const val BOTTOM_TODAY = "bottom_today"
    const val BOTTOM_INVENTORY = "bottom_inventory"
    const val BOTTOM_SHOPPING = "bottom_shopping"
    const val BOTTOM_MORE = "bottom_more"

    const val GLOBAL_ADD = "global_add"

    // Inventory
    const val INVENTORY_SCREEN = "inventory_screen"
    const val INVENTORY_LOADING = "inventory_loading"
    const val INVENTORY_EMPTY_STATE = "inventory_empty_state"
    const val INVENTORY_EMPTY_TITLE = "inventory_empty_title"
    const val INVENTORY_EMPTY_BODY = "inventory_empty_body"
    const val INVENTORY_ADD_BUTTON = "inventory_add_button"
    const val INVENTORY_ERROR_STATE = "inventory_error_state"
    const val INVENTORY_RETRY_BUTTON = "inventory_retry_button"
    const val INVENTORY_ITEM_LIST = "inventory_item_list"
    const val INVENTORY_ITEM_ROW = "inventory_item_row"
    const val INVENTORY_ITEM_NAME = "inventory_item_name"
    const val INVENTORY_ITEM_QUANTITY = "inventory_item_quantity"

    // Add Item
    const val ADD_EXACT_ITEM_SCREEN = "add_exact_item_screen"
    const val ADD_EXACT_ITEM_BACK_BUTTON = "add_exact_item_back_button"
    const val ADD_EXACT_ITEM_NAME_INPUT = "add_exact_item_name_input"
    const val ADD_EXACT_ITEM_QUANTITY_INPUT = "add_exact_item_quantity_input"
    const val ADD_EXACT_ITEM_UNIT_SELECTOR = "add_exact_item_unit_selector"
    const val ADD_EXACT_ITEM_UNIT_OPTION_UNIT = "add_exact_item_unit_option_unit"
    const val ADD_EXACT_ITEM_UNIT_OPTION_GRAM = "add_exact_item_unit_option_gram"
    const val ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM = "add_exact_item_unit_option_kilogram"
    const val ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER = "add_exact_item_unit_option_milliliter"
    const val ADD_EXACT_ITEM_UNIT_OPTION_LITER = "add_exact_item_unit_option_liter"
    const val ADD_EXACT_ITEM_NAME_ERROR = "add_exact_item_name_error"
    const val ADD_EXACT_ITEM_QUANTITY_ERROR = "add_exact_item_quantity_error"
    const val ADD_EXACT_ITEM_DUPLICATE_ERROR = "add_exact_item_duplicate_error"
    const val ADD_EXACT_ITEM_STORAGE_ERROR = "add_exact_item_storage_error"
    const val ADD_EXACT_ITEM_SAVE_BUTTON = "add_exact_item_save_button"
    const val ADD_EXACT_ITEM_CANCEL_BUTTON = "add_exact_item_cancel_button"
    const val ADD_EXACT_ITEM_SAVING = "add_exact_item_saving"

    val staticTags: List<String> =
        listOf(
            APP_ROOT,
            SCREEN_FOUNDATION,
            SCREEN_ONBOARDING,
            ONBOARDING_START,
            BOTTOM_TODAY,
            BOTTOM_INVENTORY,
            BOTTOM_SHOPPING,
            BOTTOM_MORE,
            GLOBAL_ADD,
            INVENTORY_SCREEN,
            INVENTORY_LOADING,
            INVENTORY_EMPTY_STATE,
            INVENTORY_EMPTY_TITLE,
            INVENTORY_EMPTY_BODY,
            INVENTORY_ADD_BUTTON,
            INVENTORY_ERROR_STATE,
            INVENTORY_RETRY_BUTTON,
            INVENTORY_ITEM_LIST,
            INVENTORY_ITEM_ROW,
            INVENTORY_ITEM_NAME,
            INVENTORY_ITEM_QUANTITY,
            ADD_EXACT_ITEM_SCREEN,
            ADD_EXACT_ITEM_BACK_BUTTON,
            ADD_EXACT_ITEM_NAME_INPUT,
            ADD_EXACT_ITEM_QUANTITY_INPUT,
            ADD_EXACT_ITEM_UNIT_SELECTOR,
            ADD_EXACT_ITEM_UNIT_OPTION_UNIT,
            ADD_EXACT_ITEM_UNIT_OPTION_GRAM,
            ADD_EXACT_ITEM_UNIT_OPTION_KILOGRAM,
            ADD_EXACT_ITEM_UNIT_OPTION_MILLILITER,
            ADD_EXACT_ITEM_UNIT_OPTION_LITER,
            ADD_EXACT_ITEM_NAME_ERROR,
            ADD_EXACT_ITEM_QUANTITY_ERROR,
            ADD_EXACT_ITEM_DUPLICATE_ERROR,
            ADD_EXACT_ITEM_STORAGE_ERROR,
            ADD_EXACT_ITEM_SAVE_BUTTON,
            ADD_EXACT_ITEM_CANCEL_BUTTON,
            ADD_EXACT_ITEM_SAVING,
        )
}
