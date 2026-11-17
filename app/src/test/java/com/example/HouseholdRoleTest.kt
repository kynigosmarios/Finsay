package com.example

import com.example.data.model.HouseholdRole
import com.example.util.Localization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseholdRoleTest {

    @Test
    fun `household role permissions match three tier spec`() {
        assertTrue(HouseholdRole.CREATOR.canManageMembersAndCategories)
        assertTrue(HouseholdRole.CREATOR.canManageSharingAndHousehold)

        assertTrue(HouseholdRole.EDITOR.canManageMembersAndCategories)
        assertFalse(HouseholdRole.EDITOR.canManageSharingAndHousehold)

        assertFalse(HouseholdRole.REGISTAR.canManageMembersAndCategories)
        assertFalse(HouseholdRole.REGISTAR.canManageSharingAndHousehold)
    }

    @Test
    fun `fromRaw maps creator editor registar and legacy aliases cleanly`() {
        assertEquals(HouseholdRole.CREATOR, HouseholdRole.fromRaw("REGISTAR", isCreator = true))
        assertEquals(HouseholdRole.CREATOR, HouseholdRole.fromRaw("OWNER", isCreator = false))
        assertEquals(HouseholdRole.CREATOR, HouseholdRole.fromRaw("CREATOR", isCreator = false))
        assertEquals(HouseholdRole.EDITOR, HouseholdRole.fromRaw("editor", isCreator = false))
        assertEquals(HouseholdRole.REGISTAR, HouseholdRole.fromRaw("REGISTAR", isCreator = false))
        assertEquals(HouseholdRole.REGISTAR, HouseholdRole.fromRaw("REGISTRAR", isCreator = false))
        assertEquals(HouseholdRole.REGISTAR, HouseholdRole.fromRaw(null, isCreator = false))
    }

    @Test
    fun `localization role labels return exact Greek and English strings`() {
        assertEquals("Δημιουργός", Localization.householdRoleLabel(HouseholdRole.CREATOR, Localization.Language.GREEK))
        assertEquals("Συντάκτης", Localization.householdRoleLabel(HouseholdRole.EDITOR, Localization.Language.GREEK))
        assertEquals("Καταχωρητής", Localization.householdRoleLabel(HouseholdRole.REGISTAR, Localization.Language.GREEK))

        assertEquals("Creator", Localization.householdRoleLabel(HouseholdRole.CREATOR, Localization.Language.ENGLISH))
        assertEquals("Editor", Localization.householdRoleLabel(HouseholdRole.EDITOR, Localization.Language.ENGLISH))
        assertEquals("Registar", Localization.householdRoleLabel(HouseholdRole.REGISTAR, Localization.Language.ENGLISH))
    }
}
