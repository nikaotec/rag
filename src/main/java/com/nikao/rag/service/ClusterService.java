package com.nikao.rag.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import smile.clustering.KMeans;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClusterService {

    private final FileProcessingService fileProcessingService;
    private final EmbeddingService embeddingService;

    private Map<Integer, List<ChunkData>> clusterMap = new HashMap<>();
    private KMeans kmeansModel;

    public ClusterService(FileProcessingService fileProcessingService, EmbeddingService embeddingService) {
        this.fileProcessingService = fileProcessingService;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void inicializarClusters() {
        try {
            System.out.println("📥 Iniciando clusterização de chunks...");
            String texto = fileProcessingService.loadDefaultText(
                    "https://raw.githubusercontent.com/nikaotec/teste_chat/main/adventista.pdf");
            List<String> palavras = List.of("História", "Doutrinas", "Endereço", "Horários", "Missão", "ADRA",
                    "Educação");

            List<String> chunks = fileProcessingService.splitSmartChunks(texto, palavras);
            List<ChunkData> embeddings = chunks.stream()
                    .map(chunk -> new ChunkData(chunk, embeddingService.embed(chunk).block()))
                    .filter(c -> c.embedding() != null && !c.embedding().isEmpty())
                    .toList();

            double[][] vetores = embeddings.stream()
                    .map(c -> c.embedding().stream().mapToDouble(Float::doubleValue).toArray())
                    .toArray(double[][]::new);

            kmeansModel = KMeans.fit(vetores, 5);

            for (int i = 0; i < vetores.length; i++) {
                int cluster = kmeansModel.y[i];
                clusterMap.computeIfAbsent(cluster, c -> new ArrayList<>()).add(embeddings.get(i));
            }

            System.out.printf("✅ Clusters criados com sucesso (%d clusters, %d chunks).\n", kmeansModel.k,
                    embeddings.size());

        } catch (Exception e) {
            System.err.println("❌ Erro ao inicializar clusters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Mono<List<String>> buscarChunksMaisRelevantes(String prompt) {
        return embeddingService.embed(prompt).map(vetor -> {
            double[] entrada = vetor.stream().mapToDouble(Float::doubleValue).toArray();

            int melhorCluster = -1;
            double maiorSimilaridade = -1;

            for (int i = 0; i < kmeansModel.centroids.length; i++) {
                double sim = embeddingService.cosineSimilarity(entrada, kmeansModel.centroids[i]);
                if (sim > maiorSimilaridade) {
                    maiorSimilaridade = sim;
                    melhorCluster = i;
                }
            }

            List<String> chunksRelevantes = clusterMap.getOrDefault(melhorCluster, List.of()).stream()
                    .map(ChunkData::text)
                    .collect(Collectors.toList());

            // === Sistema inteligente de temas adicionais ===
            Map<String, List<String>> mapaTopicos = Map.of(
                    "endereco", List.of("endereço", "local", "localização", "onde", "rua", "bairro", "cidade", "lugar", "localidade", "destino", "posicao", "rota", 
                            "edereco", "enderecoo", "endereçoo", "endereç", "edereço", "enderço", "endereco",
                            "endereço",
                            "endereco"),
                    "horario", List.of("horário","horários", "hora", "quando", "culto", "às", "reunião", "evento", "programação", "agenda", "tempo", "cronograma", "agenda", "periodo", "duração",
                            "orario", "horariio", "horari", "horarrio", "horaio", "horaro", "horári", "horari0",
                            "horari0",
                            "h0rario"),
                    "educacao", List.of("educação", "escola", "ensino", "aprendizado", "formação", "curso"),
                    "doutrina", List.of("doutrina", "crença", "fundamento", "ensino", "fé"),
                    "historia", List.of("história", "origem", "surgimento", "começo"),
                    "missao", List.of("missão", "evangelismo", "propósito", "visão"),
                    "profecia", List.of("profecia", "ellen", "apocalipse", "visão"), 
                    "comunidade", List.of("igreja", "comunidade", "templo"));

            Set<String> palavrasPrompt = Arrays.stream(prompt.toLowerCase().split("\\s+"))
                    .collect(Collectors.toSet());

            Set<String> temasDetectados = mapaTopicos.entrySet().stream()
                    .filter(e -> e.getValue().stream().anyMatch(palavrasPrompt::contains))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            for (String tema : temasDetectados) {
                List<String> extras = clusterMap.values().stream()
                        .flatMap(List::stream)
                        .filter(c -> mapaTopicos.get(tema).stream()
                                .anyMatch(sinonimo -> c.text().toLowerCase().contains(sinonimo)))
                        .map(ChunkData::text)
                        .filter(chunk -> !chunksRelevantes.contains(chunk))
                        .limit(3) // pegar mais de 1 se houver
                        .toList();

                if (!extras.isEmpty()) {
                    System.out.printf("📌 Chunk relacionado ao tema '%s' adicionado manualmente ao contexto.%n", tema);
                    chunksRelevantes.addAll(extras);
                }
            }

            return chunksRelevantes;
        });
    }

    public record ChunkData(String text, List<Float> embedding) {
    }

    public void recriarClustersComTexto(String texto) {
        try {
            System.out.println("⚙️ Gerando palavras-chave dinamicamente...");

            // 🔍 Gera palavras-chave com base em frequência
            List<String> palavrasChaveDinamicas = extrairPalavrasChave(texto, 10);
            System.out.println("🔑 Palavras-chave detectadas: " + palavrasChaveDinamicas);

            List<String> chunks = fileProcessingService.splitSmartChunks(texto, palavrasChaveDinamicas);

            List<ChunkData> embeddings = chunks.stream()
                    .map(chunk -> new ChunkData(chunk, embeddingService.embed(chunk).block()))
                    .filter(c -> c.embedding() != null && !c.embedding().isEmpty())
                    .toList();

            double[][] vetores = embeddings.stream()
                    .map(c -> c.embedding().stream().mapToDouble(Float::doubleValue).toArray())
                    .toArray(double[][]::new);

            kmeansModel = KMeans.fit(vetores, 5);
            clusterMap.clear();

            for (int i = 0; i < vetores.length; i++) {
                int cluster = kmeansModel.y[i];
                clusterMap.computeIfAbsent(cluster, c -> new ArrayList<>()).add(embeddings.get(i));
            }

            System.out.printf("✅ Clusters RECRIADOS com %d chunks.%n", embeddings.size());

        } catch (Exception e) {
            System.err.println("❌ Erro ao recriar clusters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> extrairPalavrasChave(String texto, int limite) {
        Map<String, Integer> frequencia = new HashMap<>();

        String[] palavras = texto.toLowerCase()
                .replaceAll("[^a-zA-Záéíóúâêôãõçà\\s]", "")
                .split("\\s+");

        Set<String> stopwords = Set.of(
                "de", "da", "do", "em", "que", "o", "a", "e", "para", "com", "uma", "os", "as", "no", "na", "se", "é",
                "por",
                "um", "dos", "das", "ao", "à", "sua", "são", "tem", "mais", "como", "ou", "também");

        for (String p : palavras) {
            if (p.length() > 3 && !stopwords.contains(p)) {
                frequencia.put(p, frequencia.getOrDefault(p, 0) + 1);
            }
        }

        return frequencia.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limite)
                .map(Map.Entry::getKey)
                .toList();
    }

}
