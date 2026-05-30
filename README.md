# Validador de Inventário — Ycon Inteligência e Tecnologia
### Projeto de Extensão — ADS · Estácio

---

## Visão Geral

Aplicativo Android nativo (Kotlin) para registro e controle de movimentação de estoque em ambientes logísticos **sem conectividade** (Offline-First). Desenvolvido como parte da Atividade de Extensão em parceria com a **Ycon Inteligência e Tecnologia**, Sorocaba-SP.

O app registra dois tipos de movimento — **Entrada** e **Saída** — e calcula o saldo líquido do estoque em tempo real, com histórico completo e rastreabilidade de todas as operações.

---

## Arquitetura

```
app/
└── src/
    ├── main/
    │   ├── java/com/ycon/validadorinventario/
    │   │   ├── data/
    │   │   │   ├── entity/
    │   │   │   │   ├── ProdutoEntity.kt              ← movimento de ENTRADA ou SAIDA
    │   │   │   │   └── TermoPersonalizadoEntity.kt   ← termos personalizados do campo "Outro"
    │   │   │   ├── dao/
    │   │   │   │   ├── ProdutoDao.kt                 ← consultas com validação em tempo de compilação
    │   │   │   │   └── TermoPersonalizadoDao.kt      ← autocomplete reativo
    │   │   │   ├── db/
    │   │   │   │   └── AppDatabase.kt                ← banco de dados Singleton (versão 2)
    │   │   │   └── ProdutoRepository.kt              ← padrão Repository
    │   │   └── ui/
    │   │       ├── adapter/
    │   │       │   └── ProdutoAdapter.kt             ← RecyclerView com DiffUtil
    │   │       ├── InventarioViewModel.kt            ← AndroidViewModel + LiveData
    │   │       └── MainActivity.kt                  ← observa LiveData, delega ao ViewModel
    │   └── res/
    │       ├── layout/
    │       │   ├── activity_main.xml                 ← painel, formulário, busca e histórico
    │       │   └── item_produto.xml                  ← item do histórico com indicador ENTRADA/SAÍDA
    │       └── values/
    │           ├── colors.xml    ← paleta de cores Estácio
    │           ├── strings.xml   ← textos e listas de opções
    │           └── themes.xml    ← tema Material Components
    ├── test/
    │   └── InventarioViewModelLogicTest.kt   ← 9 testes unitários JVM
    └── androidTest/
        ├── ProdutoDaoTest.kt                 ← 13 testes instrumentados (Room em memória)
        └── TermoPersonalizadoDaoTest.kt      ← 5 testes instrumentados (termos personalizados)
```

---

## Tecnologias Utilizadas

| Componente | Tecnologia | Justificativa |
|---|---|---|
| Linguagem | Kotlin 1.9 | Segurança contra nulos, coroutines nativas, menos código repetitivo |
| Persistência | Room 2.6 + SQLite | ORM com validação de SQL em tempo de compilação |
| Processamento em segundo plano | Coroutines + viewModelScope | Evita travamentos na tela (ANR) |
| Reatividade | LiveData + MediatorLiveData | Atualização automática da UI; busca combina dois fluxos |
| Arquitetura | MVVM + Repository | Separação de responsabilidades e facilidade de testes |
| Build | KSP (Kotlin Symbol Processing) | Geração de código Room mais rápida que KAPT |
| Testes | JUnit 4 + Room Testing | Ciclo TDD: Vermelho → Verde → Refatorar |

---

## Funcionalidades

### Registro de Movimentos
- **Tipo de movimento**: botões de alternância ENTRADA / SAÍDA no topo do formulário
- Botão e borda do formulário mudam de cor (verde para ENTRADA, coral para SAÍDA)
- **Validação de saldo negativo**: SAÍDA é bloqueada se a quantidade solicitada superar o saldo atual do SKU
- Mensagem clara distingue entre "SKU sem estoque" e "quantidade acima do disponível"

### Formulário de Registro
- **Código SKU**: campo de texto livre, convertido automaticamente para maiúsculas
- **Quantidade**: campo numérico com indicador "un." integrado
- **Setor**: dropdown Material Design com as opções do armazém
  - Opção **"Outro"**: ao selecionar, aparece um campo de texto com sugestões baseadas em valores já usados
  - Valores digitados são normalizados (sem espaços extras, em maiúsculas) e salvos para reutilização futura
- Todos os campos são obrigatórios — o registro é bloqueado enquanto algum estiver vazio

### Painel de Métricas (tempo real)

| Card | Cálculo |
|---|---|
| Saldo Estoque | `SOMA(ENTRADAS) − SOMA(SAÍDAS)` |
| Movimentos | Contagem total de registros |
| Último Mov. | Quantidade do registro mais recente (+/−) |
| Setores | Contagem de setores distintos com registros |

### Histórico com Busca
- Todos os movimentos exibidos em ordem cronológica decrescente
- Campo de busca por SKU com filtro em memória via `MediatorLiveData` (sem consultas extras ao banco)
- Mensagem diferenciada para "histórico vazio" e "nenhum resultado para a busca"
- Cada item exibe: ícone do tipo, SKU, setor, data/hora e quantidade com prefixo +/−
- Cores do item seguem o tipo do movimento (verde para ENTRADA, coral para SAÍDA)

### Comportamentos Automáticos
- Formulário é limpo após cada registro bem-sucedido
- Métricas do painel atualizam em sincronia com o histórico (sem condição de corrida)
- Mensagens de erro e confirmação não se repetem ao rotacionar a tela

---

## Padrão Offline-First

O app **não declara permissão de Internet** no `AndroidManifest.xml`. Toda a persistência ocorre no arquivo SQLite local `inventario_ycon.db`, gerenciado pelo Room. Isso garante:

- Funcionamento integral em galpões sem Wi-Fi ou rede móvel
- Zero latência de gravação (sem comunicação com servidor externo)
- Sem risco de perda de dados por falha de conexão

---

## Fluxo de Dados

```
Tela (Activity) — usuário seleciona ENTRADA ou SAÍDA
    │  toca em "REGISTRAR ENTRADA" ou "REGISTRAR SAÍDA"
    ▼
ViewModel.registrarLote(sku, qty, setor, tipo, setorIsCustom)
    │  Validação imediata: SKU, quantidade, setor
    │  viewModelScope.launch { }   ← executa em segundo plano
    │  [SAÍDA] → verifica saldoPorSku(sku) ≥ qty
    ▼
Repository.inserir(ProdutoEntity)
    │  [Outro] → salvarTermoCustom("SETOR", valor)
    │  atualizarMetricasSuspend()  ← aguarda antes de sinalizar sucesso
    ▼
ProdutoDao.inserirProduto()   ← gerado automaticamente pelo Room
    ▼
SQLite (inventario_ycon.db)
    │  Room emite LiveData atualizado automaticamente
    ▼
MediatorLiveData.produtosFiltrados (+ filtro de busca em memória)
    │  Thread principal
    ▼
Activity.observe { adapter.submitList(...) }
```

---

## Modelo de Dados

### Tabela `produtos`

| Coluna | Tipo | Descrição |
|---|---|---|
| id | INTEGER (PK) | Gerado automaticamente |
| sku | TEXT | Código do produto (maiúsculas) |
| qty | INTEGER | Quantidade do movimento |
| setor | TEXT | Setor onde o movimento ocorreu |
| ts | INTEGER | Data e hora do registro (milissegundos Unix) |
| tipo | TEXT | `"ENTRADA"` ou `"SAIDA"` |

### Tabela `termos_personalizados`

| Coluna | Tipo | Descrição |
|---|---|---|
| id | INTEGER (PK) | Gerado automaticamente |
| categoria | TEXT | `"SETOR"` — identifica o campo de origem |
| valor | TEXT | Termo normalizado digitado pelo usuário |

Índice único em `(categoria, valor)` — duplicatas são descartadas silenciosamente.

---

## Executando os Testes

```bash
# Testes unitários JVM (sem emulador)
./gradlew test

# Testes instrumentados (requer emulador ou dispositivo conectado)
./gradlew connectedAndroidTest
```

### Cobertura de Testes

| Arquivo | Casos | Tipo | O que valida |
|---|---|---|---|
| `ProdutoDaoTest` | 13 | Instrumentado | Inserção, ordenação, saldo ENTRADA/SAÍDA por SKU, agregações |
| `TermoPersonalizadoDaoTest` | 5 | Instrumentado | Persistência, deduplicação, isolamento por categoria, ordenação |
| `InventarioViewModelLogicTest` | 9 | Unitário JVM | Validação de SKU, quantidade e setor |

**Total: 27 casos de teste**

---

## Como Compilar

**Pré-requisitos:**
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Android SDK 34

```bash
git clone https://github.com/Ycon-projetos/ValidadorInventario.git
cd ValidadorInventario
./gradlew assembleDebug
# APK gerado em: app/build/outputs/apk/debug/app-debug.apk
```

> **Atenção:** O banco de dados está na versão 2. Ao instalar sobre uma versão anterior, o `fallbackToDestructiveMigration()` recria o banco do zero. Em produção, substituir por `addMigrations(MIGRATION_1_2)` para preservar os dados existentes.

---

## Sobre o Projeto

Desenvolvido como Atividade de Extensão do curso de **Análise e Desenvolvimento de Sistemas (ADS)** — Estácio, em parceria institucional com a **Ycon Inteligência e Tecnologia**, Sorocaba-SP.
