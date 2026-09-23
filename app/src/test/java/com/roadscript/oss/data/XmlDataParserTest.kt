package com.roadscript.oss.data

import com.roadscript.oss.data.models.*
import com.roadscript.oss.data.xml.XmlDataParser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class XmlDataParserTest {

    @Test
    fun testPersistenceRoundTrip_EnsuresNoDataLoss() {
        // 1. Préparer un jeu de données ultra-complet (Le "Véhicule de Test")
        val originalVehicle = Vehicle(
            id = "test-uuid",
            model = "1007",
            brand = "Peugeot",
            plateNumber = "AP-324-RB",
            fuelType = "Essence",
            countryCode = "FR",
            acquisitionMileage = 92000,
            mechanic = MechanicInfo(name = "Garage Test", phone = "0102030405"),
            insurance = InsuranceInfo(name = "Assurance Test", contractNumber = "POL-123"),
            photos = listOf(Attachment("path/doc1.jpg", "Carte Grise"))
        )

        val originalMileage = listOf(
            MileageReading("2026-08-20", 104412),
            MileageReading("2026-08-13", 104081)
        )

        val originalFuel = listOf(
            FuelReading("2026-08-21", 45.5, 78.20, 104500, "FR"),
            FuelReading("2026-08-10", 40.0, 70.00, 103900, "FR")
        )

        val originalEvents = listOf(
            Evenement(
                id = "evt-1",
                name = "Vidange",
                date = "2026-08-15",
                categoryKey = "entretien",
                subCategoryKey = "ent_moteur",
                cost = 150.0,
                description = "Huile 5W40",
                isMultiple = true,
                actions = listOf(EvenementAction("Filtre Huile", "entretien", "ent_moteur")),
                photos = listOf(Attachment("path/facture.jpg", "Facture"))
            )
        )

        // 2. SAUVEGARDE (Simule la fermeture de l'app ou un export)
        val outputStream = ByteArrayOutputStream()
        XmlDataParser.writeSuivi(outputStream, originalVehicle, originalMileage, originalEvents, originalFuel)
        val xmlResult = outputStream.toString("UTF-8")

        // 3. CHARGEMENT (Simule un redémarrage de l'app ou un import)
        val inputStream = ByteArrayInputStream(xmlResult.toByteArray(Charsets.UTF_8))
        val (_, parsedVeh, parsedMileage, parsedEvents, parsedFuel) = XmlDataParser.parseSuivi(inputStream)

        // 4. VÉRIFICATION D'INTÉGRITÉ (Strictement identique ?)
        assertNotNull(parsedVeh)
        assertEquals(originalVehicle.id, parsedVeh?.id)
        assertEquals(originalVehicle.model, parsedVeh?.model)
        assertEquals(originalVehicle.fuelType, parsedVeh?.fuelType)
        assertEquals(originalVehicle.mechanic.name, parsedVeh?.mechanic?.name)

        assertEquals("Nombre de relevés KM incorrect", originalMileage.size, parsedMileage.size)
        assertEquals(originalMileage[0].value, parsedMileage[0].value)

        assertEquals("Nombre de pleins incorrect", originalFuel.size, parsedFuel.size)
        assertEquals(originalFuel[0].liters, parsedFuel[0].liters, 0.001)
        assertEquals(originalFuel[0].odometer, parsedFuel[0].odometer)

        assertEquals("Nombre d'événements incorrect", originalEvents.size, parsedEvents.size)
        assertEquals(originalEvents[0].name, parsedEvents[0].name)
        assertEquals(originalEvents[0].actions.size, parsedEvents[0].actions.size)
    }

    @Test
    fun testSpecialCharactersPersistence() {
        val specialName = "Révision & Contrôle <Test> 🚗"
        val vehicle = Vehicle(model = specialName, id = "test-special")
        
        val outputStream = ByteArrayOutputStream()
        XmlDataParser.writeSuivi(outputStream, vehicle, emptyList(), emptyList(), emptyList())
        
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val (_, parsedVeh, _, _, _) = XmlDataParser.parseSuivi(inputStream)
        
        assertEquals(specialName, parsedVeh?.model)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseInvalidRootSuiviThrows() {
        val xmlContent = "<invalidRoot></invalidRoot>"
        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        XmlDataParser.parseSuivi(inputStream)
    }
}
