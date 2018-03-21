
package algoritmo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import controle.Constantes;

public class Poupador extends ProgramaPoupador {

	enum Opcoes {
		SEM_VISAO(-2), FORA_DO_AMBIENTE(-1), CELULA_VAZIA(0), PAREDE(1), BANCO(3), MOEDA(4), PASTILHA_DO_PODER(
				5), POUPADOR(100), LADRAO(200), PESO_POR_VISITA(-50), PASSEOU_NO_MAPA(20);

		Opcoes(int valor) {
			this.valor = valor;
		}

		private int valor;

		public int getValor() {
			return valor;
		}
	}

	private final Collection<Integer> cima = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
	private final Collection<Integer> direita = Arrays.asList(12, 13, 3, 4, 8, 9, 17, 18, 22, 23);
	private final Collection<Integer> baixo = Arrays.asList(14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
	private final Collection<Integer> esquerda = Arrays.asList(10, 11, 0, 1, 5, 6, 14, 15, 19, 20);
	private final List<Integer> possibilidades = Arrays.asList(6, 7, 8, 11, 12, 15, 16, 17);
	private final HashMap<Point, Integer> locaisVisitados = new HashMap<Point, Integer>();
	private final HashMap<String, Integer> mapa = new HashMap<String, Integer>();
	private int[] visao;
	private int[] pesos;
	private Point pontoAnterior;
	private int acaoAnterior;
	private final ArrayList<Integer> historico = new ArrayList<Integer>();
	private int tempoSemPegarMoedas;
	private int historicoDeMoedas;

	public int acao() {
		pesos = new int[24]; // Agente tem 24 possibilidades.
		reduzirOTempoNoMapa();
		pesoAtual(sensor.getPosicao());
		carregarHistoricoDeVisitacao();
		olharOAmbiente();
		usarOOlfato();
		return movimentar();
	}

	/**
	 * Verifico por onde eu já passei, e vou removendo essas possibilidades do mapa.
	 */
	private void reduzirOTempoNoMapa() {
		List<String> removidos = new ArrayList<String>();
		mapa.forEach((k, v) -> {
			v = v - 1;
			if (v == 0) {
				removidos.add(k);
			}
		});
		removidos.forEach((v) -> mapa.remove(v));
	}

	/**
	 * Verifica o peso atual, baseado no nas experiências.
	 * 
	 * @param p
	 */
	private void pesoAtual(Point p) {
		if (locaisVisitados.containsKey(p))
			locaisVisitados.put(p, locaisVisitados.get(p) - Opcoes.PESO_POR_VISITA.getValor());
		else // Colocamos negativo para, pois passamos por ele.
			locaisVisitados.put(p, Opcoes.PESO_POR_VISITA.getValor());

		mapa.put(sensor.getPosicao().x + "" + sensor.getPosicao().y, Opcoes.PASSEOU_NO_MAPA.getValor());
	}

	/**
	 * Carrega o historico de visitas
	 */
	private void carregarHistoricoDeVisitacao() {
		Point p = sensor.getPosicao();
		Point cima = new Point(p.x, p.y - 1);
		Point baixo = new Point(p.x, p.y + 1);
		Point esquerda = new Point(p.x - 1, p.y);
		Point direita = new Point(p.x + 1, p.y);
		pesos[7] += (mapa.get(cima.x + "" + cima.y) == null ? 0 : calculoDoPeso(cima, cima));
		pesos[12] += (mapa.get(direita.x + "" + cima.y) == null ? 0 : calculoDoPeso(direita, cima));
		pesos[16] += (mapa.get(baixo.x + "" + baixo.y) == null ? 0 : calculoDoPeso(baixo, baixo));
		pesos[11] += (mapa.get(esquerda.x + "" + esquerda.y) == null ? 0 : calculoDoPeso(esquerda, esquerda));
	}

	private int calculoDoPeso(Point point, Point point2) {
		return Opcoes.PESO_POR_VISITA.getValor() * mapa.get(point.x + "" + point2.y);
	}

	public void olharOAmbiente() {
		visao = sensor.getVisaoIdentificacao();
		for (int i = 0; i < visao.length; i++) {
			switch (visao[i]) {
			case 5: // Pastilha do poder
				pesos[i] += -800;
				break;
			case 4: // Moeda
				pesos[i] += 2600;
				break;
			case 3: // Banco
				pesos[i] += 500 * (sensor.getNumeroDeMoedas());
				break;
			case 1: // Parede
				pesos[i] += -600;
				break;
			case -1: // Fora do Ambiente
				pesos[i] += -600;
				break;
			case -2: // Sem visão
				pesos[i] += -200;
				break;
			default:
				if (visao[i] >= 100) {
					if (sensor.getNumeroDeMoedas() == 0) {
						pesos[i] += 2000;
					} else {
						pesos[i] += -12000;
					}
				} else {
					pesos[i] += -5;
				}
			}
		}
	}

	private void usarOOlfato() {
		usarOOlfatoLadrao();
		usarOOlfatoPoupador();
	}

	private void usarOOlfatoLadrao() {
		int[] olfato = sensor.getAmbienteOlfatoLadrao();
		for (int i = 0; i < olfato.length; i++) {
			pesos[possibilidades.get(i)] += (olfato[i] == 0) ? 1000 : -1000 * (5 - olfato[i]);
			if (sensor.getNumeroDeMoedas() == 0) {
				pesos[possibilidades.get(i)] += (2000 * (5 - olfato[i]));
			}
		}
	}

	private void usarOOlfatoPoupador() {
		int[] olfato = sensor.getAmbienteOlfatoPoupador();
		for (int i = 0; i < olfato.length; i++) {
			pesos[possibilidades.get(i)] += (olfato[i] == 0) ? 1000 : (-500 * (5 - olfato[i]));
		}
	}

	public int movimentar() {
		if ((historicoDeMoedas == sensor.getNumeroDeMoedas()) && (sensor.getNumeroDeMoedas() > 0)) {
			tempoSemPegarMoedas++;
		} else {
			tempoSemPegarMoedas = 0;
		}

		// considerar a posição do banco
		Point banco = Constantes.posicaoBanco;
		if (banco.x < sensor.getPosicao().x) {
			pesos[11] += sensor.getNumeroDeMoedas() * (70 + tempoSemPegarMoedas);
		}

		if (banco.x > sensor.getPosicao().x) {
			pesos[12] += sensor.getNumeroDeMoedas() * (70 + tempoSemPegarMoedas);
		}

		if (banco.y < sensor.getPosicao().y) {
			pesos[7] += sensor.getNumeroDeMoedas() * (70 + tempoSemPegarMoedas);
		}

		if (banco.y > sensor.getPosicao().y) {
			pesos[16] += sensor.getNumeroDeMoedas() * (70 + tempoSemPegarMoedas);
		}

		// resumir os pesos para apenas as 4 direções possíveis de movimento

		int pesoEsquerda = somarPesos(esquerda) + pesoObstaculo(11);
		int pesoDireita = somarPesos(direita) + pesoObstaculo(12);
		int pesoCima = somarPesos(cima) + pesoObstaculo(7);
		int pesoBaixo = somarPesos(baixo) + pesoObstaculo(16);

		int[] pesosDirecao = { pesoCima, pesoBaixo, pesoDireita, pesoEsquerda };

		// se a ação anterior foi ineficaz, reduz o peso para ela

		if ((pontoAnterior != null) && (pontoAnterior.equals(sensor.getPosicao()))) {
			historico.add(acaoAnterior);
			for (int i : historico) {
				pesosDirecao[i - 1] += -6000;
			}
		} else {
			historico.clear();
		}

		int maiorPeso = -999999;
		int direcao = -1;

		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] > maiorPeso) {
				maiorPeso = pesosDirecao[i];
				direcao = i + 1;
			}
		}

		ArrayList<Integer> pesosIguais = new ArrayList<Integer>();
		// verifica se tem mais de um maiorPeso
		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] == maiorPeso) {
				pesosIguais.add(i + 1);
			}
		}

		if (pesosIguais.size() > 1) {
			int indice = (int) (Math.random() * (pesosIguais.size()));
			direcao = pesosIguais.get(indice);

		}

		pontoAnterior = sensor.getPosicao();
		acaoAnterior = direcao;
		historicoDeMoedas = sensor.getNumeroDeMoedas();

		/*
		 * os valores de retorno são 0: ficar parado 1: ir pra cima 2: ir pra baixo 3:
		 * ir pra direita 4: ir pra esquerda
		 */

		// System.out.println(direcao + " " + pesoCima + " " + pesoBaixo + " " +
		// pesoDireita + " " + pesoEsquerda + " " + sensor.getPosicao());

		if ((direcao < 1) || (direcao > 4)) {
			return 0;
		}
		return direcao;
	}

	private int somarPesos(Collection<Integer> direcao) {
		int soma = 0;
		for (int i : direcao) {
			soma += pesos[i];
		}
		return soma;
	}

	private int pesoObstaculo(int posicao) {
		if ((visao[posicao] == Opcoes.PAREDE.getValor()) || (visao[posicao] == Opcoes.FORA_DO_AMBIENTE.getValor())
				|| (visao[posicao] >= 100)) {
			return -5000;
		} else if ((visao[posicao] == Opcoes.PASTILHA_DO_PODER.getValor())) {
			return -6000;
		} else if ((visao[posicao] == Opcoes.BANCO.getValor()) && (sensor.getNumeroDeMoedas() == 0)) {
			return -5000;
		} else {
			return 0;
		}
	}
}
