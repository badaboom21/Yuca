package com.example.yuca.bdd.service;

import java.nio.file.Path;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OpenFoodFactsImportRunner implements CommandLineRunner {

    private final OpenFoodFactsImportService importService;

    public OpenFoodFactsImportRunner(OpenFoodFactsImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java -jar ... <chemin-vers-le-fichier.csv>");
            return;
        }

        Path csvPath = Path.of(args[0]);
        importService.importCsv(csvPath);
    }
}
