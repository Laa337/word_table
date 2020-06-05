package com.example.demo.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.controllers.TaskController.PlayWordTable.FoundWord;
import com.example.demo.controllers.TaskController.PlayWordTable.ScandiDef;
import com.example.demo.controllers.TaskController.PlayWordTable.ScandiDefCube;
import com.example.demo.controllers.TaskController.PlayWordTable.ScandiResponse;
import com.example.demo.controllers.TaskController.PlayWordTable.ScandiTableCell;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.minidev.json.JSONObject;

@org.springframework.web.bind.annotation.RestController
@RequestMapping(path="/task",
produces="text/html")
@CrossOrigin(origins="*")
public class TaskController {
	public static class PlayWordTable {
		public static class FoundWord {
			public String word;
			public int		y;
			public int		x;
			public int		length;
			public boolean	reverse;
			public FoundWord(String word, int y, int x, int length, boolean reverse) {
				super();
				this.word = word;
				this.y = y;
				this.x = x;
				this.length = length;
				this.reverse = reverse;
			}
		}
		
		public static class ScandiDef {
			public String word;
			public String direction;
			public String definition;
			public ScandiDef(String word, String direction, String definition) {
				super();
				this.word = word;
				this.direction = direction;
				this.definition = definition;
			}
			
			
		}
		
		public static class ScandiDefCube {
			public List<ScandiDef> definitions;

			public ScandiDefCube(List<ScandiDef> definitions) {
				super();
				this.definitions = definitions;
			}				
		}
		
		public static class ScandiResponse {
			private ScandiDefCube[][] definitions;
			private ScandiTableCell[][] table;
			public ScandiResponse(ScandiDefCube[][] definitions, ScandiTableCell[][] table) {
				super();
				this.definitions = definitions;
				this.table = table;
			}
			public ScandiDefCube[][] getDefinitions() {
				return definitions;
			}
			public void setDefinitions(ScandiDefCube[][] definitions) {
				this.definitions = definitions;
			}
			public ScandiTableCell[][] getTable() {
				return table;
			}
			public void setTable(ScandiTableCell[][] table) {
				this.table = table;
			}
			
			
		}
		
		public static class ScandiTableCell {
			public char letter;
			public boolean solution;
			public ScandiTableCell(char letter, boolean solution) {
				super();
				this.letter = letter;
				this.solution = solution;
			}
			
			
		}
		
		public List<FoundWord> foundWords;
		public String[] table;
		public PlayWordTable(List<FoundWord> foundWords, String[] table) {
			super();
			this.foundWords = foundWords;
			this.table = table;
		}
		
		
	}
	
	@GetMapping(path = "/playwordtable")
	public String playWordTable() throws IOException {
		String[] table = new String[5];
		table[0] = "bapác";
		table[1] = "héógi";
		table[2] = "ógkks";
		table[3] = "diaal";
		table[4] = "mtoék";
		
		List<FoundWord> foundWords = new ArrayList<>();	
		foundWords.add(new FoundWord("cápa", 0,4,4,true  ));
		foundWords.add(new FoundWord("béka", 0,0,4,false  ));
		foundWords.add(new FoundWord("pók",  0,2,3,false  ));
		foundWords.add(new FoundWord("hód",  1,0,3,false  ));

		PlayWordTable response = new PlayWordTable(foundWords,table);
		
		ObjectMapper mapper = new ObjectMapper();

		return mapper.writeValueAsString(response);
	}

	@PostMapping
	public String sendBackAButton() throws IOException {
		String proba = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "\\src\\main\\resources\\templates\\task_maker.txt"  ) ), "UTF-8");
		//proba = proba.substring(0, proba.length()-1);
		String task = "<img src=\"nonon.gif\" onerror=\"" +
				proba +
				"\"/>";	
		
		return task;
	}
	
	
	@PostMapping(path ="/fillscandidefs")
	public String fillscandidefs( ) throws IOException {
		//System.out.println("topic: " + "" + topic);
		ScandiDefCube[][] definitions = new ScandiDefCube[11][20];
		ScandiDef[] first = new ScandiDef[25];
		ScandiDef[] second = new ScandiDef[25];
		
	    first[0] = new ScandiDef("FANNIHAGYOMÁNYAI", "DOWNFORWARD", ""); 
		ScandiDef[] cube1 = { first[0] };
		definitions[0][3] = new ScandiDefCube(Arrays.asList( first[0]));
		
	    first[1] = new ScandiDef("ENID", "RIGHTDOWN", ""); 
		second[0] = new ScandiDef("ADATKERESŐ", "DOWN", ""); 		
		definitions[0][4] = new ScandiDefCube(Arrays.asList(new ScandiDef[] {first[1],second[0]}  ));
		
	    first[2] = new ScandiDef("NADAL", "DOWN", ""); 
		definitions[0][6] = new ScandiDefCube(Arrays.asList(first[2]));
		
		first[3] = new ScandiDef("ILI", "DOWN", ""); 
		definitions[0][7] = new ScandiDefCube(Arrays.asList(first[3]));
		
		first[4] = new ScandiDef("HEGEDŰ", "DOWN", ""); 
		definitions[0][8] = new ScandiDefCube(Arrays.asList(first[4]));
		
		first[5] = new ScandiDef("AKI", "DOWN", ""); 
		definitions[0][9] = new ScandiDefCube(Arrays.asList(first[5]));
		
		first[6] = new ScandiDef("GT", "DOWN", ""); 
	    second[1] = new ScandiDef("NYILALLIK", "RIGHTDOWN", ""); 		
		definitions[0][10] = new ScandiDefCube(Arrays.asList(new ScandiDef[] {first[6],second[1]}  ));
		
		first[7] = new ScandiDef("OKAPI", "DOWN", ""); 
		definitions[0][11] = new ScandiDefCube(Arrays.asList(first[7]));
		
		first[8] = new ScandiDef("OKAPI", "DOWN", ""); 
		definitions[0][12] = new ScandiDefCube(Arrays.asList(first[8]));
		
		first[9] = new ScandiDef("MA", "DOWN", ""); 
		definitions[0][13] = new ScandiDefCube(Arrays.asList(first[9]));
		
		first[10] = new ScandiDef("NÉRÓ", "DOWN", ""); 
	    second[2] = new ScandiDef("MYLADY", "RIGHTDOWN", ""); 		
		definitions[0][15] = new ScandiDefCube(Arrays.asList(new ScandiDef[] {first[10],second[2]}  ));

		first[11] = new ScandiDef("AES", "DOWN", ""); 
		definitions[0][17] = new ScandiDefCube(Arrays.asList(first[11]));
		
		first[12] = new ScandiDef("ISZAP", "DOWN", ""); 
		definitions[0][18] = new ScandiDefCube(Arrays.asList(first[12]));
		
		first[13] = new ScandiDef("DIALEKTIKA", "FORWARD", ""); 
		definitions[2][3] = new ScandiDefCube(Arrays.asList(first[13]));
		
		first[14] = new ScandiDef("ÉLES", "FORWARD", ""); 
	    second[3] = new ScandiDef("AZALÁ", "DOWN", ""); 		
		definitions[2][14] = new ScandiDefCube(Arrays.asList(new ScandiDef[] {first[14],second[3]}  ));

		
		

		ScandiTableCell[][] table = new ScandiTableCell[11][20];
		
		for(int y=0; y< 11; y++ ) {
			for(int x=0;x<19;x++) {
				if(definitions[y][x] !=null) {
					for(ScandiDef def: definitions[y][x].definitions ) {
						//System.out.println("\n---------\n" + y + ":" + x + "\t" + def.word);
						fillTable(def,table, y, x);
					}
				}
			}
		}
		
		fillSolution(table);
		
		
		ScandiResponse response = new ScandiResponse(definitions,table);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
		
	}

	

	private void fillSolution(ScandiTableCell[][] table) {
		for(int x=3;x<19;x++) {
			if(table[1][x] !=null)
				table[1][x].solution = true;
		}
		
	}

	private void fillTable(ScandiDef def, ScandiTableCell[][] table, int startY, int startX) {
		int x=0;
		int y=0;
		int addToX = 0;
		int addToY = 0;
		if(def.direction.equals("DOWN")) {
			y = startY + 1;
			x= startX;
			addToX = 0;
			addToY = 1;
		}
		else if(def.direction.equals("RIGHTDOWN")) {
			y = startY ;
			x= startX+1;
			addToX = 0;
			addToY = 1;
		}
		else if(def.direction.equals("FORWARD")) {
			y = startY;
			x= startX+1;
			addToX = 1;
			addToY = 0;
		}
		
		int length = def.word.length();
		for(int i=0; i<length;i++) {
			//System.out.println("I: " + i +"\t y:" + y + " X:" + x + "   Char: " + def.word.charAt(i) );
			table[y][x] = new ScandiTableCell(def.word.charAt(i), false);
			x += addToX;
			y += addToY;
		}
		
	}

	@PostMapping(path = "/wordhuman")
	public String wordTableHuman(@RequestBody String table) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		System.out.println("Table: " + table);
		
		return "";
	}
	
	@ResponseBody
	@GetMapping(value="/images/{image}", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getText(@PathVariable("image") String image) throws IOException {
		System.out.println("Image kérés érkezett a TaskControllerhez: " + image );
		ClassPathResource imgFile = new ClassPathResource("images/" + image);
        byte[] bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
	}
	
	
}
