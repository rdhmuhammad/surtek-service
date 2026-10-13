package com.sabarceria.surtek.commons;

import com.sabarceria.surtek.exceptions.InvalidDataException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder(builderClassName = "UtilBuilder")
public class ExcelUtil {

    private final Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

    private final Pattern CELL_PATTERN = Pattern.compile("^([A-Z]+)(\\d+)$");

    static final List<String> ACCEPTED_CONTENT_TYPE = Arrays.asList(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private Map<Class<?>, Map<String, String>> classCache;

    public static class UtilBuilder {
        public UtilBuilder addClass(Class<?> clazz) {
            this.classCache.put(clazz, getMappedField(clazz));
            return this;
        }
    }

    public static UtilBuilder builder() {
        return new UtilBuilder()
                .classCache(new ConcurrentHashMap<>());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CellAddress {
        private String letters;
        private String numbers;
    }

    private CellAddress splitCellAddress(String cellAddress) {
        Matcher matcher = CELL_PATTERN.matcher(cellAddress);

        if (matcher.matches()) {
            String letters = matcher.group(1);
            String numbers = matcher.group(2);
            return new CellAddress(letters, numbers);
        } else {
            throw new IllegalArgumentException("Invalid cell address format: " + cellAddress);
        }
    }

    public <T> List<T> readExcel(MultipartFile data, String sheetName, Class<T> returnType) throws IOException {
        if (!ACCEPTED_CONTENT_TYPE.contains(data.getContentType())) {
            throw new InvalidDataException("Invalid content type: " + data.getContentType());
        }

        Map<String, String> cellHeaders = new HashMap<>();
        XSSFWorkbook workbook = new XSSFWorkbook(data.getInputStream());
        XSSFSheet sheet = workbook.getSheet(sheetName);
        Iterator<Row> rows = sheet.iterator();

        List<T> results = new ArrayList<>();
        int rowNumber = 0;
        while (rows.hasNext()) {
            Row currentRow = rows.next();


            Iterator<Cell> cells = currentRow.iterator();
            T result = BeanUtils.instantiateClass(returnType);
            while (cells.hasNext()) {
                Cell currentCell = cells.next();
                if (rowNumber == 0) {
                    cellHeaders.put(splitCellAddress(currentCell.getAddress().formatAsString()).getLetters(), currentCell.getStringCellValue());
                    continue;
                }


                setFieldByColumnName(result, cellHeaders.getOrDefault(splitCellAddress(currentCell.getAddress().formatAsString()).getLetters(), ""), currentCell.getStringCellValue());
            }

            T emptyResult = BeanUtils.instantiateClass(returnType);
            if (rowNumber != 0 && Objects.equals(emptyResult, result)) {
                break;
            }
            if (rowNumber != 0) results.add(result);
            rowNumber++;

        }

        workbook.close();

        return results;

    }

    private static Map<String, String> getMappedField(Class<?> clazz) {
        Map<String, String> mappedFields = new ConcurrentHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            String fromExcel = field.getAnnotation(SheetColumn.class).value();
            String fromClass = field.getName();
            mappedFields.put(fromExcel, fromClass);
        }

        return mappedFields;
    }


    public void setFieldByColumnName(Object object, String excelColumnName, Object value) {
        Class<?> clazz = object.getClass();
        if (String.valueOf(value).isEmpty()) return;
        Map<String, String> fieldMapped = classCache.get(clazz);
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
        wrapper.setPropertyValue(fieldMapped.get(excelColumnName), value);
    }

}
