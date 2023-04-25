import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.shop.CSVParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CSVParserTest {

    private static final String SHOP_FILE = "src/test/java/shop.csv";

    @Test
    public void testParser() throws IOException {
        String csv = Files.readString(Path.of(SHOP_FILE));
        List<List<String>> parsedCsv = CSVParser.parseCSV(csv);
        System.out.println(parsedCsv);
    }
}
