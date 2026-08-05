package com.example.suivivehicule.data

import com.example.suivivehicule.data.models.*
import com.example.suivivehicule.data.xml.XmlDataParser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class XmlDataParserTest {

    @Test
    fun testParseValidCategories() {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <categories>
                <category key="entretien" icon="ic_maintenance">
                    <name>Entretien</name>
                    <description>Opérations d'entretien</description>
                </category>
                <subcategory key="vidange" categoryKey="entretien" icon="ic_oil">
                    <name>Vidange</name>
                    <description>Vidange moteur</description>
                </subcategory>
            </categories>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val (categories, subCategories) = XmlDataParser.parseCategories(inputStream)

        assertEquals(1, categories.size)
        assertEquals("entretien", categories[0].key)
        assertEquals("Entretien", categories[0].name)
        assertEquals("ic_maintenance", categories[0].icon)

        assertEquals(1, subCategories.size)
        assertEquals("vidange", subCategories[0].key)
        assertEquals("entretien", subCategories[0].categoryKey)
        assertEquals("Vidange", subCategories[0].name)
    }

    @Test
    fun testWriteAndParseCategories() {
        val cats = listOf(
            Category("test_cat", "Test Catégorie", "Desc", "ic_test")
        )
        val subs = listOf(
            SubCategory("test_sub", "test_cat", "Test Sous-Catégorie", "Desc Sub", "ic_sub")
        )

        val outputStream = ByteArrayOutputStream()
        XmlDataParser.writeCategories(outputStream, cats, subs)

        val xmlBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(xmlBytes)
        val (parsedCats, parsedSubs) = XmlDataParser.parseCategories(inputStream)

        assertEquals(1, parsedCats.size)
        assertEquals("test_cat", parsedCats[0].key)
        assertEquals("Test Catégorie", parsedCats[0].name)

        assertEquals(1, parsedSubs.size)
        assertEquals("test_sub", parsedSubs[0].key)
        assertEquals("test_cat", parsedSubs[0].categoryKey)
        assertEquals("Test Sous-Catégorie", parsedSubs[0].name)
    }

    @Test
    fun testParseValidSuivi() {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <suivi vehicle_id="v-123">
                <vehicule>
                    <modele>Golf 8</modele>
                    <marque>Volkswagen</marque>
                    <immatriculation>AB-123-CD</immatriculation>
                    <date_premiere_immat>2021-06-15</date_premiere_immat>
                    <vin>WVWZZZXXXXXXXXXXX</vin>
                    <genre>VP</genre>
                    <puissance_fiscale>6</puissance_fiscale>
                    <carburant>Essence</carburant>
                    <photo_path>/path/to/car.jpg</photo_path>
                </vehicule>
                <kilometres>
                    <releve date="2026-06-20">45000</releve>
                    <releve date="2026-05-15">43500</releve>
                </kilometres>
                <evenements>
                    <evenement id="act-1">
                        <nom>Changement Filtres</nom>
                        <date>2026-06-10</date>
                        <category_key>entretien</category_key>
                        <subcategory_key>filtres</subcategory_key>
                        <cout>45.90</cout>
                        <description>Filtre habitacle et air</description>
                        <photos>
                            <photo>garage/v-123/photo1.jpg</photo>
                        </photos>
                    </evenement>
                </evenements>
            </suivi>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val (_, vehicle, mileage, actions, _) = XmlDataParser.parseSuivi(inputStream)

        assertNotNull(vehicle)
        assertEquals("Golf 8", vehicle!!.model)
        assertEquals("Volkswagen", vehicle.brand)
        assertEquals(6, vehicle.fiscalPower)

        assertEquals(2, mileage.size)
        assertEquals("2026-06-20", mileage[0].date)
        assertEquals(45000, mileage[0].value)

        assertEquals(1, actions.size)
        assertEquals("act-1", actions[0].id)
        assertEquals("Changement Filtres", actions[0].name)
        assertEquals(45.90, actions[0].cost, 0.01)
        assertEquals(1, actions[0].photos.size)
        assertEquals("garage/v-123/photo1.jpg", actions[0].photos[0].path)
    }

    @Test
    fun testWriteAndParseSuivi() {
        val vehicle = Vehicle(model = "208", brand = "Peugeot", plateNumber = "XX-123-XX")
        val mileage = listOf(MileageReading("2026-06-20", 98000))
        val evenements = listOf(Evenement("act-123", "Freins", "2026-06-19", "entretien", "freins", 120.0, "Plaquettes avant"))

        val outputStream = ByteArrayOutputStream()
        XmlDataParser.writeSuivi(outputStream, vehicle, mileage, evenements, emptyList())

        val xmlBytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(xmlBytes)
        val (_, parsedVeh, parsedMil, parsedAct, _) = XmlDataParser.parseSuivi(inputStream)

        assertNotNull(parsedVeh)
        assertEquals("208", parsedVeh!!.model)
        assertEquals("Peugeot", parsedVeh.brand)

        assertEquals(1, parsedMil.size)
        assertEquals(98000, parsedMil[0].value)

        assertEquals(1, parsedAct.size)
        assertEquals("act-123", parsedAct[0].id)
        assertEquals(120.0, parsedAct[0].cost, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseInvalidRootCategoriesThrows() {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <badRoot></badRoot>
        """.trimIndent()
        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        XmlDataParser.parseCategories(inputStream)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseInvalidRootSuiviThrows() {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <badRoot></badRoot>
        """.trimIndent()
        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        XmlDataParser.parseSuivi(inputStream)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseSuiviMissingVehicleIdThrows() {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <suivi>
                <vehicule><modele>Test</modele></vehicule>
            </suivi>
        """.trimIndent()
        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        XmlDataParser.parseSuivi(inputStream)
    }
}
