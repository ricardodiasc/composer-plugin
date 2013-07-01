/**
 * 
 */
package org.jenkinsci.plugins.composerplugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author Ricardo Dias Cavalcante
 *
 */
public class ComposerCommand {
	
	/**
	 * Validate instalation in case of global
	 * 
	 */
	private void validateInstalation(){
		
	}
	
	/**
	 * Execute install command in a build
	 * @throws IOException 
	 */
	public void install() throws IOException{
//		ProcessBuilder  pb = new ProcessBuilder("bash", "-c", "ls");
//		ProcessBuilder  pb = new ProcessBuilder("/home/ricardo/java/nexus/nexus-2.4.0-09/bin/nexus", "console");
		ProcessBuilder  pb = new ProcessBuilder();
		
		List<String> listCommands = new ArrayList<String>();
		listCommands.add("/home/ricardo/java/nexus/nexus-2.4.0-09/bin/nexus");
		listCommands.add("console");
		pb.command(listCommands);
		
		
//		pb.command("ls");
		Process process = pb.start();
		
		OutputStreamWriter osw = new OutputStreamWriter(process.getOutputStream());
		InputStreamReader isr = new InputStreamReader(process.getInputStream());
		
		BufferedReader br = new BufferedReader(isr);
		BufferedWriter bw = new BufferedWriter(osw);
		
		PrintStream out = System.out;
		String linha = null;
		int i=0;
//		process.exitValue();
		while((linha = br.readLine() ) !=null){
			out.println(linha);
		}
		
		process.exitValue();
	}
	
	public static void main(String args[]){
		ComposerCommand composerCommand = new ComposerCommand();
		try{
			composerCommand.install();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
