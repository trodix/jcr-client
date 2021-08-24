package com.trodix.jcr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.trodix.jcr.client.services.JackrabbitService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JackRabbitTests {

	@Test
	void test_valid_intervals_getDateIntervals() throws ParseException {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date dateDebut = df.parse("01/01/2021");
		Date dateFin = df.parse("01/02/2021");
		int chunk = 10;

		int expectedDateRangeCount = 4;

		Date expectedDateDebutInterval1 = dateDebut;
		Date expectedDateFinInterval1 = df.parse("11/01/2021");

		Date expectedDateDebutInterval2 = df.parse("12/01/2021");
		Date expectedDateFinInterval2 = df.parse("21/01/2021");

		Date expectedDateDebutInterval3 = df.parse("22/01/2021");
		Date expectedDateFinInterval3 = df.parse("31/01/2021");

		Date expectedDateDebutInterval4 = df.parse("01/02/2021");
		Date expectedDateFinInterval4 = dateFin;

		Map<Date, Date> result = JackrabbitService.getDateIntervals(dateDebut, dateFin, chunk);

		assertEquals(expectedDateRangeCount, result.size(), "Le nombre de range de date doit être égal à " + expectedDateRangeCount);

		assertTrue(result.containsKey(dateDebut), "La date de début doit être présente dans la liste");

		assertTrue(result.containsKey(expectedDateDebutInterval1));
		assertTrue(result.containsValue(expectedDateFinInterval1));

		assertTrue(result.containsKey(expectedDateDebutInterval2));
		assertTrue(result.containsValue(expectedDateFinInterval2));

		assertTrue(result.containsKey(expectedDateDebutInterval3));
		assertTrue(result.containsValue(expectedDateFinInterval3));

		assertTrue(result.containsKey(expectedDateDebutInterval4));
		assertTrue(result.containsValue(expectedDateFinInterval4));

		assertTrue(result.containsValue(dateFin), "La date de fin doit être présente dans la liste");
	}

	@Test
	void test_start_dates_are_ordered_getDateIntervals() throws ParseException {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date dateDebut = df.parse("01/01/2021");
		Date dateFin = df.parse("31/01/2021");
		int chunk = 10;

		Map<Date, Date> result = JackrabbitService.getDateIntervals(dateDebut, dateFin, chunk);

		Set<Date> set = result.keySet();
		List<Date> listKeys = new ArrayList<>(set);
		List<Date> listValues = new ArrayList<>(result.values());
		Date firstItem = listKeys.get(0);
		Date lastItem = listValues.get(listValues.size() - 1);

		assertEquals(dateDebut, firstItem, "La première date doit être la date de début");

		for (int i = 0; i < listKeys.size() - 1; i++) {
			Date curentDate;
			Date nextDate;

			curentDate = listKeys.get(i);
			nextDate = listKeys.get(i + 1);
			assertTrue(nextDate.after(curentDate), "Les dates doivent être classées du plus ancien au plus récent");
		}

		assertEquals(dateFin, lastItem, "La dernière date doit être la date de fin");
	}

}
