package br.com.conversor;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class AccountStyleLinkConverterWithLog {

    public static void main(String[] args) throws Exception {
        String producaoFile = "C:\\Users\\andepint\\OneDrive - Capgemini\\PPMC 85426 - Automatização área Sustentabilidade\\ONDA_2\\ENVIZI\\20250502\\Flat_File_Format_02052025.csv";
        String homologacaoFile = "C:\\Users\\andepint\\OneDrive - Capgemini\\PPMC 85426 - Automatização área Sustentabilidade\\ONDA_2\\ENVIZI\\SANDBOX\\Flat_File_Format_09052025_sandbox.csv";
        String etlFile = "C:\\Users\\andepint\\OneDrive - Capgemini\\PPMC 85426 - Automatização área Sustentabilidade\\ONDA_2\\ENVIZI\\SAIDAS - 20250509\\Account_Setup_and_Data_Load_-_PM&C_AGUA_ENERGIA_ALPHABUILDING.csv";
        String outputFile = "C:\\Users\\andepint\\OneDrive - Capgemini\\PPMC 85426 - Automatização área Sustentabilidade\\ONDA_2\\ENVIZI\\SAIDAS - 20250509\\arquivo_etl_convertido.csv";
        String logFile = "C:\\Users\\andepint\\OneDrive - Capgemini\\PPMC 85426 - Automatização área Sustentabilidade\\ONDA_2\\ENVIZI\\SAIDAS - 20250509\\inconsistencias.log";

        Map<String, String> mapLinkToCaption = new HashMap<>();
        Map<String, String> mapCaptionToNewLink = new HashMap<>();

        try (
                CSVReader prodReader = new CSVReader(new FileReader(producaoFile));
                CSVReader homReader = new CSVReader(new FileReader(homologacaoFile));
                PrintWriter logWriter = new PrintWriter(logFile)
        ) {
            // 1. Mapeia: Account Style Link (prod) → Caption
            String[] prodHeader = prodReader.readNext();
            int idxProdLink = findIndex(prodHeader, "Account Style Link");
            int idxProdCaption = findIndex(prodHeader, "Account Style Caption");

            String[] linha;
            while ((linha = prodReader.readNext()) != null) {
                if (!linha[idxProdLink].isEmpty() && !linha[idxProdCaption].isEmpty()) {
                    mapLinkToCaption.put(linha[idxProdLink], linha[idxProdCaption]);
                }
            }

            // 2. Mapeia: Caption → Account Style Link (homologação)
            String[] homHeader = homReader.readNext();
            int idxHomLink = findIndex(homHeader, "Account Style Link");
            int idxHomCaption = findIndex(homHeader, "Account Style Caption");

            while ((linha = homReader.readNext()) != null) {
                if (!linha[idxHomCaption].isEmpty() && !linha[idxHomLink].isEmpty()) {
                    mapCaptionToNewLink.put(linha[idxHomCaption], linha[idxHomLink]);
                }
            }

            // 3. Processa ETL
            try (
                    CSVReader etlReader = new CSVReader(new FileReader(etlFile));
                    CSVWriter writer = new CSVWriter(new FileWriter(outputFile))
            ) {
                String[] etlHeader = etlReader.readNext();
                writer.writeNext(etlHeader);
                int idxEtlLink = findIndex(etlHeader, "Account Style Link");

                while ((linha = etlReader.readNext()) != null) {
                    String oldLink = linha[idxEtlLink];
                    String caption = mapLinkToCaption.get(oldLink);

                    if (caption == null) {
                        logWriter.println("[FALHA] Link não encontrado na produção: " + oldLink);
                    } else {
                        String newLink = mapCaptionToNewLink.get(caption);
                        if (newLink == null) {
                            logWriter.println("[FALHA] Caption não encontrado na homologação: " + caption + " (Link: " + oldLink + ")");
                        } else {
                            linha[idxEtlLink] = newLink;
                        }
                    }

                    writer.writeNext(linha);
                }
            }
        }

        System.out.println("Conversão concluída. Saída: " + outputFile);
        System.out.println("Inconsistências salvas em: " + logFile);
    }

    private static int findIndex(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new RuntimeException("Coluna não encontrada: " + columnName);
    }
}
