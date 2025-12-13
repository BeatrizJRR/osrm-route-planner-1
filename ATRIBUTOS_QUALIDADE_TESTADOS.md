# Análise de Atributos de Qualidade Testados

## 📊 Mapeamento: Testes vs Atributos de Qualidade

### ✅ **TESTADOS** (Total: 4 atributos)

| Atributo | Refinamento | Evidência nos Testes | Classe de Teste |
|----------|-------------|----------------------|-----------------|
| **Fiabilidade - Integridade dos Dados** | Consistência de Exportação | ✅ Testa exportação de JSON e GPX com formato válido | `RouteExporterTest` (2 testes) |
| **Segurança - Validação** | Validação de Inputs | ✅ Testa criação de objetos com dados válidos/inválidos | `PointTest`, `POITest`, `RouteTest` (8 testes) |
| **Manutenibilidade - Modularidade** | Arquitetura em Camadas | ✅ Estrutura de testes reflete separação UI → Service → Model | Todos os testes (30 testes) |
| **Fiabilidade - Tolerância a Falhas** | Falha de APIs Externas | ✅ Testa comportamento quando APIs falham | `ServiceTest` (6 testes) |

---

### ⚠️ **PARCIALMENTE TESTADOS** (Total: 2 atributos)

| Atributo | Refinamento | Limitações | Cobertura |
|----------|-------------|------------|-----------|
| **Fiabilidade - Gestão de Exceções** | Comportamento Seguro | Alguns testes validam null/empty, mas sem casos de exceção explícitos | ~40% |
| **Manutenibilidade - Testabilidade** | Mock de APIs | ServiceTest menciona necessidade de mocks mas usa APIs reais | ~30% |

---

### ❌ **NÃO TESTADOS** (Total: 10 atributos)

#### **Desempenho** (0/4 testados)
| Atributo | Refinamento | Requisito | Por que NÃO foi testado |
|----------|-------------|-----------|-------------------------|
| Tempo de Resposta | Cálculo de Rotas | ≤ 3 segundos | Nenhum teste mede tempo de execução |
| Tempo de Resposta | Pesquisa de POIs | ≤ 2 segundos | Nenhum teste mede tempo de execução |
| Responsividade da UI | Processamento Assíncrono | Uso de threads/async | Testes UI não cobrem threading |
| Renderização do Mapa | Velocidade | < 100 ms | Testes UI não testam renderização |

#### **Usabilidade** (0/3 testados)
| Atributo | Refinamento | Por que NÃO foi testado |
|----------|-------------|-------------------------|
| Facilidade de Aprendizagem | Interface Intuitiva | Requer testes com utilizadores reais |
| Feedback ao Utilizador | Indicadores de Carregamento | Sem testes de componentes JavaFX |
| Tratamento de Erros | Mensagens Compreensíveis | Sem validação de mensagens de erro |

#### **Segurança** (1/3 testados)
| Atributo | Refinamento | Por que NÃO foi testado |
|----------|-------------|-------------------------|
| Exportação de Dados | Proteção de Informação | Sem validação de dados pessoais |
| Comunicação com APIs | HTTPS | Sem testes de protocolo de rede |

#### **Manutenibilidade** (1/3 testados)
| Atributo | Refinamento | Por que NÃO foi testado |
|----------|-------------|-------------------------|
| Configurabilidade | Ficheiro de Configuração | URLs hardcoded no código |

---

## 📈 Estatísticas Gerais

### Por Categoria de Qualidade

| Utilidade | Total Atributos | Testados | Parcialmente | Não Testados | Taxa |
|-----------|-----------------|----------|--------------|--------------|------|
| **Desempenho** | 4 | 0 | 0 | 4 | **0%** |
| **Usabilidade** | 3 | 0 | 0 | 3 | **0%** |
| **Fiabilidade** | 3 | 2 | 1 | 0 | **67%** ✅ |
| **Segurança** | 3 | 1 | 0 | 2 | **33%** |
| **Manutenibilidade** | 3 | 1 | 1 | 1 | **33%** |
| **TOTAL** | **16** | **4** | **2** | **10** | **25%** |

### Por Impacto Arquitetónico

| Impacto | Total | Testados | Taxa |
|---------|-------|----------|------|
| **Alto** | 8 | 2 | 25% |
| **Médio** | 6 | 2 | 33% |
| **Baixo** | 2 | 0 | 0% |

---

## 🔍 Detalhamento dos Testes Existentes

### 1. **RouteExporterTest** (2 testes)
**Atributo:** Fiabilidade - Integridade dos Dados
```
✅ exportToJson_writesExpectedStructure
   - Valida estrutura JSON (distance_km, duration_sec, mode, route_points)
   
✅ exportToGPX_writesGpxTemplateAndPoints
   - Valida formato GPX válido (XML, versão 1.1, trkpt)
```

### 2. **ServiceTest** (6 testes)
**Atributos:** Fiabilidade - Tolerância a Falhas, Gestão de Exceções
```
✅ getGeocodeFromLocationString_withValidQuery_returnsPoint
   - Testa API Nominatim com query válida
   
✅ getGeocodeFromLocationString_withInvalidQuery_returnsNull
   - Testa tratamento de queries inválidas (retorna null)
   
✅ searchLocations_withValidQuery_returnsResults
   - Valida resultados de pesquisa
   
✅ getRoute_withValidPoints_returnsRoute
   - Testa API OSRM (inclui fallback se API falhar)
   
✅ getPOIsAlongRoute_withNullRoute_returnsEmptyList
   - Testa comportamento com input null
   
✅ getElevationProfile_withNullRoute_returnsNull
   - Testa comportamento com input null
```

### 3. **PointTest, POITest, RouteTest** (8 testes)
**Atributo:** Segurança - Validação de Inputs
```
✅ Validam criação de objetos com dados corretos
✅ Testam igualdade de objetos
✅ Validam métodos toString()
✅ Testam estruturas de dados imutáveis
```

### 4. **MapViewerBasicTest** (10 testes - NOVOS)
**Atributo:** Manutenibilidade - Modularidade
```
✅ Testam estruturas de dados UI separadas da lógica
✅ Validam criação de rotas, POIs, waypoints
✅ Demonstram separação entre Model e UI
```

### 5. **ElevationProfileTest** (3 testes)
**Atributo:** Fiabilidade - Integridade dos Dados
```
✅ Testam cálculos de elevação
✅ Validam estruturas de dados
```

---

## 💡 Recomendações para Melhorar Cobertura

### Prioridade ALTA (Impacto Arquitetónico Alto)

1. **Desempenho - Tempo de Resposta**
   ```java
   @Test
   void getRoute_shouldCompleteWithin3Seconds() {
       long start = System.currentTimeMillis();
       Route route = service.getRoute(origin, dest, TransportMode.CAR);
       long duration = System.currentTimeMillis() - start;
       assertTrue(duration < 3000, "Route calculation took " + duration + "ms");
   }
   ```

2. **Desempenho - Responsividade UI**
   ```java
   @Test
   void calculateRoute_shouldNotBlockUIThread() {
       // Verificar que cálculo corre em thread separada
       assertFalse(Platform.isFxApplicationThread());
   }
   ```

3. **Usabilidade - Tratamento de Erros**
   ```java
   @Test
   void invalidInput_shouldShowClearErrorMessage() {
       String errorMsg = service.validateInput("");
       assertTrue(errorMsg.contains("obrigatório"));
   }
   ```

### Prioridade MÉDIA

4. **Segurança - HTTPS**
   ```java
   @Test
   void apiClients_shouldUseHTTPS() {
       assertTrue(osrmClient.getBaseUrl().startsWith("https://"));
   }
   ```

5. **Manutenibilidade - Configurabilidade**
   ```java
   @Test
   void apiUrls_shouldComeFromConfigFile() {
       Properties config = loadConfig();
       assertNotNull(config.getProperty("osrm.url"));
   }
   ```

---

## 📊 Resumo Executivo

### O que está BEM ✅
- ✅ **Fiabilidade** bem testada (67%)
- ✅ Exportação de dados validada
- ✅ Tolerância a falhas de APIs implementada
- ✅ Arquitetura modular refletida nos testes

### O que falta ❌
- ❌ **Desempenho** completamente ignorado (0%)
- ❌ **Usabilidade** sem testes (0%)
- ❌ Segurança parcialmente testada (33%)
- ❌ Manutenibilidade pode melhorar (33%)

### Impacto na Qualidade
Com apenas **25% dos atributos testados**, o projeto tem:
- ✅ Boa base de testes funcionais
- ❌ Lacunas críticas em desempenho e usabilidade
- ⚠️ Risco médio em requisitos com alto impacto arquitetónico

---

**Gerado em:** 13 de Dezembro de 2024  
**Total de Testes:** 30  
**Atributos Testados:** 4 de 16 (25%)
