# Clínica Bem Estar — Sistema de Gestão

Trabalho de Programação Orientada a Objetos (POO) em Java. O sistema automatiza o
agendamento de consultas, o cadastro de médicos, especialidades e pacientes, o cálculo
do valor das consultas (de acordo com a especialidade e o tipo de paciente), o
histórico clínico com prescrições eletrônicas, o registro de pagamentos e o relatório
de faturamento por tipo de paciente.

## Como executar

Requisitos: **JDK 17+** e **Maven 3.8+**.

```bash
mvn package
```

```bash
java -jar target/clinica.jar
```

Ao iniciar, o sistema pergunta se deve carregar **dados de exemplo** (4 especialidades,
5 médicos com horários, 4 pacientes e algumas consultas futuras), o que facilita a
demonstração.

Para rodar apenas os testes automatizados:

```bash
mvn test
```

Sem Maven, também é possível compilar e executar só com o JDK:

```bash
mkdir -p out && javac -d out $(find src/main/java -name "*.java") && java -cp out br.com.bemestar.clinica.Main
```

## Requisitos atendidos

| Requisito | Onde está implementado |
|---|---|
| Cadastro de especialidades com valor base e associação de médicos | `CadastroService`, `Especialidade`, `Medico` |
| Cadastro de horários de atendimento dos médicos | `HorarioAtendimento`, `Medico#definirHorario` |
| Agendamento por médico ou por especialidade, em horários livres de 30 min, sem dois agendamentos no mesmo horário para o mesmo médico | `AgendaService#agendar`, `#agendarPorEspecialidade`, `#horariosDisponiveis` |
| Encaixes emergenciais: máximo de 2 por médico por dia, com acréscimo de 20% | `AgendaService#agendarEncaixe`, `PoliticaPrecos` |
| Cancelamento com menos de 24h de antecedência cobra taxa de 50% | `AgendaService#cancelar`, `PoliticaPrecos#calcularTaxaCancelamento` |
| Pacientes Particular (valor cheio), Convênio (20% de desconto) e VIP (30% de desconto) | `PacienteParticular`, `PacienteConvenio`, `PacienteVip` |
| Cálculo automático do valor por especialidade e tipo de paciente | `PoliticaPrecos#calcularValorConsulta` |
| Histórico de consultas e diagnósticos | `AgendaService#registrarRealizacao`, `ProntuarioService#historicoDoPaciente` |
| Prescrições eletrônicas vinculadas a consultas já realizadas | `ProntuarioService#emitirPrescricao`, `Prescricao`, `ItemPrescricao` |
| Registro de pagamento de cada consulta | `FaturamentoService#registrarPagamento`, `Pagamento` |
| Relatório de faturamento por tipo de paciente | `FaturamentoService#gerarRelatorio`, `RelatorioFaturamento` |

## Regras de negócio e decisões de projeto

- **Duração e grade:** toda consulta dura 30 minutos. Os horários de uma consulta normal
  são os slots de 30 min contados a partir do início do turno do médico naquele dia da semana.
- **Conflitos:** um médico não pode ter duas consultas normais no mesmo slot, e um paciente
  não pode ter duas consultas agendadas que se sobreponham. Não é possível agendar no passado.
- **Encaixes:** por definição, um encaixe pode ocupar um horário já agendado, mas precisa
  estar dentro do turno do médico e não pode coincidir com outro encaixe. Encaixes
  cancelados liberam a vaga.
- **Valor:** é calculado e fixado no momento do agendamento:
  `valor base da especialidade × (1 − desconto do paciente) × (1,20 se for encaixe)`.
  Exemplo para Cardiologia (R$ 300,00): Particular R$ 300,00, Convênio R$ 240,00,
  VIP R$ 210,00; como encaixe: R$ 360,00, R$ 288,00 e R$ 252,00.
- **Cancelamento:** permitido apenas para consultas agendadas que ainda não começaram. Se a
  antecedência for menor que 24h, é cobrada taxa de 50% do valor da consulta; com 24h ou
  mais, não há taxa. Consultas canceladas liberam o horário.
- **Pagamento:** registrado para consultas realizadas (valor integral) ou canceladas com
  taxa (valor da taxa). Cada consulta pode ser paga uma única vez.
- **Faturamento:** soma dos pagamentos, agrupada por tipo de paciente, para todo o período
  ou para um intervalo de datas.
- **Valores monetários** usam `BigDecimal` com duas casas decimais, evitando erros de
  arredondamento de `double`.
- **Persistência** em memória (os dados existem enquanto o programa está em execução).

## Conceitos de POO aplicados

- **Encapsulamento:** as entidades protegem o seu estado. Por exemplo, `Consulta` só muda de
  status pelos métodos `cancelar`, `registrarRealizacao` e `registrarPagamento`, que
  validam as transições permitidas.
- **Herança:** `Pessoa` → `Medico` e `Paciente`; `Paciente` → `PacienteParticular`,
  `PacienteConvenio` e `PacienteVip`.
- **Abstração e polimorfismo:** `Paciente` é abstrata e cada subclasse define
  `getTipo()` e `getPercentualDesconto()`; o cálculo de preço não precisa saber qual é o
  tipo concreto do paciente.
- **Interfaces e generics:** `Repositorio<T extends Identificavel>`, com a implementação
  `RepositorioEmMemoria<T>`; os serviços dependem da interface, não da implementação.
- **Composição e associação:** `Medico` tem uma `Especialidade` e uma agenda de
  `HorarioAtendimento`; `Consulta` agrega `Prescricao` e `Pagamento`.
- **Enums com comportamento**, `record`s imutáveis (`HorarioAtendimento`, `ItemPrescricao`,
  `RelatorioFaturamento`) e **exceções** próprias (`RegraNegocioException`,
  `EntidadeNaoEncontradaException`).
- **Injeção de dependência** do relógio (`java.time.Clock`), o que permite testar as regras
  de 24h e de "não agendar no passado" com datas fixas.

## Estrutura do projeto

```
src/main/java/br/com/bemestar/clinica
├── Main.java                 ponto de entrada
├── Clinica.java              monta repositórios e serviços
├── modelo/                   entidades e regras do domínio
├── repositorio/              Repositorio<T> e RepositorioEmMemoria<T>
├── servico/                  CadastroService, AgendaService, ProntuarioService,
│                             FaturamentoService, PoliticaPrecos, HistoricoPaciente
├── relatorio/                RelatorioFaturamento
├── excecao/                  exceções do domínio
└── console/                  interface de texto (menus), leitura de dados, dados de exemplo
src/test/java/...             testes JUnit 5 das regras de negócio
```

## Diagrama de classes (simplificado)

```mermaid
classDiagram
    class Pessoa {
        <<abstract>>
        -long id
        -String nome
        -String cpf
        -String telefone
    }
    class Medico {
        -String crm
        +definirHorario(HorarioAtendimento)
        +slotsNo(LocalDate, Duration)
    }
    class Paciente {
        <<abstract>>
        +getTipo() TipoPaciente
        +getPercentualDesconto() BigDecimal
        +aplicarDesconto(BigDecimal) BigDecimal
    }
    class PacienteParticular
    class PacienteConvenio {
        -String operadora
        -String numeroCarteirinha
    }
    class PacienteVip
    class Especialidade {
        -String nome
        -BigDecimal valorBase
    }
    class HorarioAtendimento {
        <<record>>
        DayOfWeek dia
        LocalTime inicio
        LocalTime fim
    }
    class Consulta {
        -LocalDateTime inicio
        -TipoConsulta tipo
        -StatusConsulta status
        -BigDecimal valor
        -String diagnostico
        -BigDecimal taxaCancelamento
        +cancelar()
        +registrarRealizacao()
        +registrarPagamento()
    }
    class Prescricao
    class ItemPrescricao {
        <<record>>
    }
    class Pagamento {
        -BigDecimal valor
        -FormaPagamento forma
    }

    Pessoa <|-- Medico
    Pessoa <|-- Paciente
    Paciente <|-- PacienteParticular
    Paciente <|-- PacienteConvenio
    Paciente <|-- PacienteVip
    Medico --> Especialidade
    Medico o-- HorarioAtendimento
    Consulta --> Paciente
    Consulta --> Medico
    Consulta o-- Prescricao
    Consulta --> Pagamento
    Prescricao o-- ItemPrescricao
```
