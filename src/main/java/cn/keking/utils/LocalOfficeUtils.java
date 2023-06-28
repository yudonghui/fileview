package cn.keking.utils;

import org.jodconverter.core.util.OSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author chenjh
 * @since 2022-12-15
 */
public class LocalOfficeUtils {

    public static final String OFFICE_HOME_KEY = "office.home";
    public static final String DEFAULT_OFFICE_HOME_VALUE = "default";

    private static final String EXECUTABLE_DEFAULT = "program/soffice.bin";
    private static final String EXECUTABLE_MAC = "program/soffice";
    private static final String EXECUTABLE_MAC_41 = "MacOS/soffice";
    private static final String EXECUTABLE_WINDOWS = "program/soffice.exe";
    private static final Logger logger = LoggerFactory.getLogger(LocalOfficeUtils.class);

    public static File getDefaultOfficeHome() {
        Properties properties = new Properties();
        String customizedConfigPath = ConfigUtils.getCustomizedConfigPath();
        logger.info("路径customizedConfigPath：{}", customizedConfigPath);
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
          /* BufferedReader bufferedReader = new BufferedReader(new FileReader(customizedConfigPath));
            properties.load(bufferedReader);*/
            ConfigUtils.restorePropertiesFromEnvFormat(properties);
            logger.info("路径getDefaultOfficeHome：{}", customizedConfigPath);
        } catch (Exception ignored) {
            logger.info("路径getDefaultOfficeHome catch：{}", ignored.toString());
        }
        String officeHome = properties.getProperty(OFFICE_HOME_KEY);
        if (officeHome != null && !DEFAULT_OFFICE_HOME_VALUE.equals(officeHome)) {
            return new File(officeHome);
        }
        if (OSUtils.IS_OS_WINDOWS) {
            String userDir = ConfigUtils.getResourcePath();
            // Try to find the most recent version of LibreOffice or OpenOffice,
            // starting with the 64-bit version. %ProgramFiles(x86)% on 64-bit
            // machines; %ProgramFiles% on 32-bit ones
            final String programFiles64 = System.getenv("ProgramFiles");
            final String programFiles32 = System.getenv("ProgramFiles(x86)");
            logger.info("路径userDir-windows：{}", userDir);
            return findOfficeHome(EXECUTABLE_WINDOWS,
                    userDir + File.separator + "libreoffice",
                    programFiles32 + File.separator + "LibreOffice",
                    programFiles64 + File.separator + "LibreOffice 7",
                    programFiles32 + File.separator + "LibreOffice 7",
                    programFiles64 + File.separator + "LibreOffice 6",
                    programFiles32 + File.separator + "LibreOffice 6",
                    programFiles64 + File.separator + "LibreOffice 5",
                    programFiles32 + File.separator + "LibreOffice 5",
                    programFiles64 + File.separator + "LibreOffice 4",
                    programFiles32 + File.separator + "LibreOffice 4",
                    programFiles32 + File.separator + "OpenOffice 4",
                    programFiles64 + File.separator + "LibreOffice 3",
                    programFiles32 + File.separator + "LibreOffice 3",
                    programFiles32 + File.separator + "OpenOffice.org 3");
        } else if (OSUtils.IS_OS_MAC) {
            logger.info("路径userDir-windows：{}", "IS_OS_MAC");
            File homeDir = findOfficeHome(EXECUTABLE_MAC_41,
                    "/Applications/LibreOffice.app/Contents",
                    "/Applications/OpenOffice.app/Contents",
                    "/Applications/OpenOffice.org.app/Contents");

            if (homeDir == null) {
                homeDir = findOfficeHome(EXECUTABLE_MAC,
                        "/Applications/LibreOffice.app/Contents",
                        "/Applications/OpenOffice.app/Contents",
                        "/Applications/OpenOffice.org.app/Contents");
            }
            return homeDir;
        } else {
            // Linux or other *nix variants
            String userDir = ConfigUtils.getResourcePath();
            String path = LocalOfficeUtils.class.getClassLoader().getResource("libreoffice").getPath();
            logger.info("路径userDir-windows：{} {}", "Linux",path);
            return findOfficeHome(EXECUTABLE_DEFAULT,
                    "/src/target/classes/libreoffice",
                    "/opt/libreoffice6.0",
                    "/opt/libreoffice6.1",
                    "/opt/libreoffice6.2",
                    "/opt/libreoffice6.3",
                    "/opt/libreoffice6.4",
                    "/opt/libreoffice7.0",
                    "/opt/libreoffice7.1",
                    "/opt/libreoffice7.2",
                    "/opt/libreoffice7.3",
                    "/opt/libreoffice7.4",
                    "/opt/libreoffice7.5",
                    "/usr/lib64/libreoffice",
                    "/usr/lib/libreoffice",
                    "/usr/local/lib64/libreoffice",
                    "/usr/local/lib/libreoffice",
                    "/opt/libreoffice",
                    "/usr/lib64/openoffice",
                    "/usr/lib64/openoffice.org3",
                    "/usr/lib64/openoffice.org",
                    "/usr/lib/openoffice",
                    "/usr/lib/openoffice.org3",
                    "/usr/lib/openoffice.org",
                    "/opt/openoffice4",
                    "/opt/openoffice.org3");
        }
    }

    /**
     * 这段代码使用了Java 8的Stream API，作用是在给定的若干个目录中查找是否存在包含指定文件名的文件，并返回一个File对象。
     * <p>
     * 解释一下这段代码的具体过程：
     * <p>
     * Stream.of(homePaths)：将homePaths数组转换成一个Stream对象，方便后续的操作。
     * filter(homePath -> Files.isRegularFile(Paths.get(homePath, executablePath)))：使用filter()方法过滤出符合条件的元素，这里是通过判断每个路径下是否存在名为executablePath的文件来进行过滤。如果存在，就保留该元素。
     * findFirst()：获取Stream中的第一个元素，这里是找到的第一个符合条件的路径。
     * map(File::new)：将找到的路径转换成File对象。
     * orElse(null)：如果没有找到符合条件的路径，则返回null。
     * 最终的结果就是一个File对象，表示存在名为executablePath的文件的目录中的第一个目录的路径。如果没有找到符合条件的目录，则返回null。
     */
    private static File findOfficeHome(final String executablePath, final String... homePaths) {
        File file = Stream.of(homePaths)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String homePath) {
                        boolean regularFile = Files.isRegularFile(Paths.get(homePath, executablePath));
                        logger.info("路径筛选：{} {}", homePath,regularFile);
                        return regularFile;
                    }
                })
                // .filter(homePath -> Files.isRegularFile(Paths.get(homePath, executablePath)))
                .findFirst()
                .map(File::new)
                .orElse(null);
        return file;
    }
}
