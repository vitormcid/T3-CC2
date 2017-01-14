package main;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import gramatica.LGraphBaseVisitor;
import gramatica.LGraphParser.Arquivo_grafoContext;
import gramatica.LGraphParser.AtribuicaoContext;
import gramatica.LGraphParser.CmdContext;
import gramatica.LGraphParser.ComandosContext;
import gramatica.LGraphParser.CorpoContext;
import gramatica.LGraphParser.DeclaracoesContext;
import gramatica.LGraphParser.EdgesContext;
import gramatica.LGraphParser.InicioContext;
import gramatica.LGraphParser.MetricaContext;
import gramatica.LGraphParser.NodesContext;
import gramatica.LGraphParser.Objeto_metricaContext;
import gramatica.LGraphParser.Parametros_createContext;
import gramatica.LGraphParser.Parametros_nodesContext;
import gramatica.LGraphParser.Parametros_updateContext;
import gramatica.LGraphParser.ProgramaContext;
import gramatica.LGraphParser.Salvar_opcionalContext;
import gramatica.LGraphParser.TipoContext;
import gramatica.LGraphParser.Tipos_tuplaContext;
import gramatica.LGraphParser.Tipos_tupla_opContext;
import gramatica.LGraphParser.TuplaContext;
import gramatica.LGraphParser.Valor_parametroContext;
import gramatica.LGraphParser.VariavelContext;
import tabelaDeSimbolos.PilhaDeTabelas;
import tabelaDeSimbolos.TabelaDeSimbolos;

public class AnalisadorSemantico extends LGraphBaseVisitor<String> {

	ArrayList<String> varsDecl = new ArrayList<String>(); //aux : guarda variaveis de mesmo tipo declaradas	
	TabelaDeSimbolos tab;//tabela de simbolos global
	PilhaDeTabelas pilhaTabs;
	SaidaParser sp;
	
	public AnalisadorSemantico(TabelaDeSimbolos t,SaidaParser sp){
		this.tab = t;
		this.sp = sp;
		this.pilhaTabs = new PilhaDeTabelas();
		this.pilhaTabs.empilhar(this.tab);
	}


	@Override
	public String visitPrograma(ProgramaContext ctx) {
	   
	   /* Declaracao de variaveis */
		if(ctx.corpo().declaracoes()!=null)
			visitDeclaracoes(ctx.corpo().declaracoes());
		
	   /* Comandos */
		visitCorpo(ctx.corpo());
	    
		return null;
	}

	@Override
	public String visitCorpo(CorpoContext ctx) {
		
		/* Comandos */
		visitComandos(ctx.comandos());
		
		return null;
	}

	@Override
	public String visitDeclaracoes(DeclaracoesContext ctx) {
		
		String tipo;
		
		/* primeiras variavies declaracadas de mesmo tipo */
		tipo = visitVariavel(ctx.dec1);
		insereVariavelTabela(tipo);
		this.varsDecl.clear();
		
		/* outras (se existirem) variveis de mesmo tipo decalradas */
		for(VariavelContext v : ctx.outrasDecs){
			tipo = visitVariavel(v);
			insereVariavelTabela(tipo);
			this.varsDecl.clear();
		}
		
		
		return null;
	}

	/* Metodo que insere variaveis declaradas na tabela de simbolos */
	public void insereVariavelTabela(String tipo){
		
		/* usa lista auxiliar varsDecl */
		for(String v : varsDecl){
			if(!tab.existeSimbolo(v)){
				tab.adicionarSimbolo(v, tipo);
				
			}else{
				sp.println("Erro: Variavel " + v + " já declarada anteriormente", "semantico");
			}
		}
		
	}
	
	
	@Override
	public String visitVariavel(VariavelContext ctx) {
		
		/* guarda em varsDecl variaveis declaradas de MESMO tipo */
		varsDecl.add(ctx.var1.getText());
		for(Token var : ctx.outrasVars){
			varsDecl.add(var.getText());
		}
		
		/* retorna tipo */
		return ctx.t.getText();
		
	}

	@Override
	public String visitComandos(ComandosContext ctx) {
		
		/* percorre lista de comandos feitos */
		for(CmdContext cmd : ctx.cmds){
			visitCmd(cmd);
		}
		
		
		return null;
	}


	@Override
	public String visitCmd(CmdContext ctx) {
		
		int comando=0; // 1 - leitura, 2 - create, 3 - update, 4 - find, 5 - atribuicao
		
		/* QUAL COMANDO E */
		List<ParseTree> filhos = ctx.children;
		
		for(ParseTree p : filhos){
			if(p.getText().equals("read")){
				comando = 1;
			     break;
			}else if(p.getText().equals("create")){
				comando = 2;
			     break;
			}else if(p.getText().equals("update")){
				comando = 3;
			     break;
			}else if(p.getText().equals("find")){
				comando = 4;
			     break;
			}else if(p.getText().equals("=")){
				comando = 5;
			     break;
			}
			
			
		}// FIM QUAL COMANDO E
		
		
		
		/* SEMANTICA : Verifica se variaveis passadas para create, read, update, find sao tipo graph e se foram declaradas */
		
		/* CREATE ou READ */
		if(comando==2 || comando==1){
			String id = filhos.get(3).getText();
			
			/* variavel graph nao declarada */
			if(!this.tab.existeSimbolo(id)){
				sp.println("Erro: variavel " + id + " não declarada", "semantico");
			}
			/* variavel nao e tipo graph */
			else if(!this.pilhaTabs.getTipo(id).equals("graph")){
				sp.println("Erro: variavel " + id + " não é do tipo graph", "semantico");
			}
			
		}/* UPDATE */
		else if(comando==3){
			String id = filhos.get(2).getText();
			
			/* variavel graph nao declarada */
			if(!this.tab.existeSimbolo(id)){
				sp.println("Erro: variavel " + id + " não declarada", "semantico");
			}
			/* variavel nao e tipo graph */
			else if(!this.pilhaTabs.getTipo(id).equals("graph")){
				sp.println("Erro: variavel " + id + " não é do tipo graph", "semantico");
			}
			
		}
		/* FIND */
		else if(comando==4){
			String id = visitObjeto_metrica(ctx.objeto_metrica());
			
			/* variavel graph nao declarada */
			if(!this.tab.existeSimbolo(id)){
				sp.println("Erro: variavel " + id + " não declarada", "semantico");
			}
			/* variavel nao e tipo graph */
			else if(!this.pilhaTabs.getTipo(id).equals("graph")){
				sp.println("Erro: variavel " + id + " não é do tipo graph", "semantico");
			}
			
		}
		
		/* FIM VERIFICACAO SEMANTICA DE GRAPH EM COMANDOS */
		
		
		/* SEMANTICA : VERIFICACAO DE DECLARACAO DE VARIAVEIS EM ATRIBUICAO */
		if(comando==5){
			
			String var_atribuicao=null;
			if(ctx.IDENT()!=null)
				var_atribuicao = ctx.IDENT().getText();//variavel que recebe o valor
			
			/* Verifica se var_atribuicao ja foi declarada */
			if(!this.pilhaTabs.existeSimbolo(var_atribuicao)){
				sp.println("Erro: variavel " + var_atribuicao + " não declarada", "semantico");
			}
			
			/* Verifica se variavel atribuida ja foi declarada */
			String var = visitAtribuicao(ctx.atribuicao());
			if(var!=null && var!="int" && var!="real" && var!="edges" && var!="nodes" && var!="string"){
				if(!this.pilhaTabs.existeSimbolo(var)){
					sp.println("Erro: variavel " + var + " não declarada", "semantico");
				}
			}
			
		}//FIM VERIFICAO SEMANTICA DECLARACAODE VARIAVEIS EM ATRBUICAO
		
		/* SEMANTICA : VERIFICACAO DE COMPATIBILIDADE EM ATRIBUICAO */
		if(comando==5){
			
			String var_atribuicao=null;
			
			if(ctx.IDENT()!=null)
				var_atribuicao = ctx.IDENT().getText();//variavel que recebe o valor
			
			String tipo_var_atr = this.pilhaTabs.getTipo(var_atribuicao);
			
			
			
			String t = visitAtribuicao(ctx.atribuicao());
			
			
			if(t=="int"){
				if(!tipo_var_atr.equals(t)){
					sp.println("Erro: incompatibilidade de tipo em atribuicao de " + var_atribuicao, "semantico");
				}
				
			}else if(t=="real"){
				if(!tipo_var_atr.equals(t)){
					sp.println("Erro: incompatibilidade de tipo em atribuicao de " + var_atribuicao, "semantico");
				}
				
			}else if(t=="edges"){
				if(!tipo_var_atr.equals(t)){
					sp.println("Erro: incompatibilidade de tipo em atribuicao de " + var_atribuicao, "semantico");
				}
				
				
			}else if(t=="nodes"){
				if(!tipo_var_atr.equals(t)){
					sp.println("Erro: incompatibilidade de tipo em atribuicao de " + var_atribuicao, "semantico");
				}
				
				
			}else if(t=="string"){
				if(!tipo_var_atr.equals(t)){
					sp.println("Erro: incompatibilidade de tipo em atribuicao de " + var_atribuicao, "semantico");
				}
				
			}else{
				//IDENT
				 t = this.pilhaTabs.getTipo(t);
				if(!tipo_var_atr.equals(t)){
					sp.println("Erro: incompatibilidade de tipo em atribuicao de " + var_atribuicao, "semantico");
				}
				
			}
			
		}	/* FIM DE VERIFICACAO DE COMPATIBILIDADE EM ATRIBUICAO */
		
		return null;
	}
	
	@Override
	public String visitAtribuicao(AtribuicaoContext ctx) {
		
		/* Retorna nome variavel atribuida se existe, senao retorna tipo */
		
		String var = null;
		
		/* CASO SEJA IDENT */
		if(ctx.IDENT()!=null)
			var = ctx.IDENT().getText();
		else if(ctx.NUM_INT()!=null)
			var = "int";
		else if(ctx.NUM_REAL()!=null)
			var = "real";
		else if(ctx.edges()!=null)
			var = "edges";
		else if(ctx.nodes()!=null)
			var = "nodes";
		else if(ctx.STRING()!=null)
			var = "string";
		
		return var;
	}
	
	@Override
	public String visitObjeto_metrica(Objeto_metricaContext ctx) {
		
		String grafo_id=null;
		boolean vertex = false;
		
		/* pega variavel graph */
		List<ParseTree> filhos = ctx.children;
	
		
		/* Verifica se e metrica de vertex ou graph */
		for(ParseTree p : filhos){
			if(p.getText().equals("vertex")){
				vertex = true;
				break;
			}
		}
		
		/* Pega nome grafo passado */
		if(vertex){
			grafo_id = filhos.get(4).getText();
			
		}else{
			grafo_id = filhos.get(1).getText();
			
		}
		
		
		return grafo_id;
	}

	@Override
	public String visitSalvar_opcional(Salvar_opcionalContext ctx) {
		// TODO Auto-generated method stub
		return super.visitSalvar_opcional(ctx);
	}

	@Override
	public String visitArquivo_grafo(Arquivo_grafoContext ctx) {
		// TODO Auto-generated method stub
		return super.visitArquivo_grafo(ctx);
	}

	@Override
	public String visitMetrica(MetricaContext ctx) {
		// TODO Auto-generated method stub
		return super.visitMetrica(ctx);
	}

	

	@Override
	public String visitParametros_create(Parametros_createContext ctx) {
		// TODO Auto-generated method stub
		return super.visitParametros_create(ctx);
	}

	@Override
	public String visitParametros_update(Parametros_updateContext ctx) {
		// TODO Auto-generated method stub
		return super.visitParametros_update(ctx);
	}

	@Override
	public String visitValor_parametro(Valor_parametroContext ctx) {
		// TODO Auto-generated method stub
		return super.visitValor_parametro(ctx);
	}

	@Override
	public String visitTipo(TipoContext ctx) {
		// TODO Auto-generated method stub
		return super.visitTipo(ctx);
	}



	@Override
	public String visitEdges(EdgesContext ctx) {
		// TODO Auto-generated method stub
		return super.visitEdges(ctx);
	}

	@Override
	public String visitTupla(TuplaContext ctx) {
		// TODO Auto-generated method stub
		return super.visitTupla(ctx);
	}

	@Override
	public String visitTipos_tupla(Tipos_tuplaContext ctx) {
		// TODO Auto-generated method stub
		return super.visitTipos_tupla(ctx);
	}

	@Override
	public String visitTipos_tupla_op(Tipos_tupla_opContext ctx) {
		// TODO Auto-generated method stub
		return super.visitTipos_tupla_op(ctx);
	}

	@Override
	public String visitParametros_nodes(Parametros_nodesContext ctx) {
		// TODO Auto-generated method stub
		return super.visitParametros_nodes(ctx);
	}

	@Override
	public String visitNodes(NodesContext ctx) {
		// TODO Auto-generated method stub
		return super.visitNodes(ctx);
	}

	



	
}