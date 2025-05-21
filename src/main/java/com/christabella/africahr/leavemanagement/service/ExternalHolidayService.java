package com.christabella.africahr.leavemanagement.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import com.christabella.africahr.leavemanagement.dto.PublicHolidayDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ExternalHolidayService {

      public List<PublicHolidayDto> fetchRwandaPublicHolidays(int year) {
            List<PublicHolidayDto> holidays = new ArrayList<>();
            try {
                  String url = "https://www.timeanddate.com/holidays/rwanda/" + year;
                  Document doc = Jsoup.connect(url).get();
                  Elements rows = doc.select("table.table--left.table--inner-borders-rows tbody tr");
                  System.out.println("Found rows: " + rows.size());
                  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

                  for (Element row : rows) {
                        Element dateCell = row.selectFirst("th");
                        Elements cols = row.select("td");
                        for (int i = 0; i < cols.size(); i++) {
                            System.out.println("col[" + i + "]: '" + cols.get(i).text() + "'");
                        }
                        if (dateCell != null && cols.size() >= 2) {
                              String dayMonth = dateCell.text().replace("\u00a0", " ").trim(); // "1 Jan"
                              String dateStr = dayMonth + " " + year; // "1 Jan 2025"
                              String name = cols.get(1).text(); // Holiday name (corrected)
                              System.out.println("Parsing: " + dateStr + " | name: " + name);
                              try {
                                    LocalDate date = LocalDate.parse(dateStr, formatter);
                                    holidays.add(PublicHolidayDto.builder()
                                                .date(date)
                                                .name(name)
                                                .build());
                              } catch (Exception e) {
                                    System.out.println("Failed to parse date for row: " + row.text() + " | dateStr: " + dateStr);
                              }
                        }
                  }
            } catch (Exception e) {
                  System.out.println("Error fetching or parsing holidays: " + e.getMessage());
            }
            return holidays;
      }
}