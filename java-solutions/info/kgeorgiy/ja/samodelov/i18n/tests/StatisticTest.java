package info.kgeorgiy.ja.samodelov.i18n.tests;

import info.kgeorgiy.ja.samodelov.i18n.TextStatistics;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

// :NOTE: не содержательные тесты
public class StatisticTest extends Assert {

    private boolean test(String fileName, String textLocale, String writeLocale) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        System.setErr(p);
        try {
            System.out.println("Test: " + fileName + "\n");
            String prefix = "java-solutions\\info\\kgeorgiy\\ja\\samodelov\\i18n\\tests\\";
            TextStatistics.main(new String[]{textLocale, writeLocale, prefix + "sources\\" + fileName + ".txt", prefix + "stats\\" + fileName + "_stat.txt"});
        } catch (Exception e) {
            return false;
        }
        return bytes.toString().isBlank();
    }

    @Test
    public void testEmpty() {
        assertTrue(test("empty", "ru-RU", "ru-RU"));
    }

    @Test
    public void testOnlyNumbers() {
        assertTrue(test("only_numbers", "ru-RU", "ru-RU"));
    }

    @Test
    public void testRuToEn() {
        assertTrue(test("easy_ru_RU", "ru-RU", "en-US"));
    }
    @Test
    public void testEasyRu() {
        assertTrue(test("easy_ru_RU", "ru-RU", "ru-RU"));
    }

    @Test
    public void testEasyEn() {
        assertTrue(test("easy_en_US", "en-US", "en-US"));
    }

    @Test
    public void testEasyAr() {
        assertTrue(test("easy_ar_AE", "ar-AE", "en-US"));
    }
    @Test
    public void testEasyAr2() {
        assertTrue(test("easy_ar_AE2", "ar-AE", "en-US"));
    }
    @Test
    public void testBigAr() {
        assertTrue(test("big_test_ar_AE", "ar-AE", "en-US"));
    }

    @Test
    public void testBigCh() {
        assertTrue(test("big_test_ch_CH", "ch-CH", "en-US"));
    }

    @Test
    public void testAdvancedNumber() {
        assertTrue(test("advanced_number_ru_RU", "ru-RU", "ru-RU"));
    }

    @Test
    public void testFailings() {
        assertFalse(test("easy_ru_RU.txt", "ch", "en-US"));
        assertFalse(test("easy_ru_RU.txt", "ru-RU", "ar-AE"));
        assertFalse(test("easy_ru_RU.txt", "ru-RU", "en-AE"));
        assertFalse(test("easy_ru_RU.txt", "en-RU", "en-US"));
        assertFalse(test("easy_ar_AE.txt", "ru-RU", "en-US"));
    }

}
