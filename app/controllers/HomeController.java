package controllers;


import actors.wordsearchgame.controllers.ScandiCrosswordCreatorController;
import actors.wordsearchgame.controllers.WordFinderController;
import actors.wordsearchgame.controllers.WordTableCreatorController;
import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.AskPattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.actorModels.wordgame.scandi.ScandiTable;
import models.actorModels.wordgame.wordtable.SelectTable;
import models.actorModels.wordgame.wordtable.WordTable;
import org.json.JSONArray;
import org.json.JSONObject;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repositories.actor.scandi.ScandiRepository;
import repositories.actor.wordtable.TableRepository;
import views.html.index;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static play.libs.Json.toJson;


/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    public final ActorRef<Object> baseRooter;
    private final ActorSystem system;
    private final ScandiRepository scandiRepository;
    private TableRepository tableRepository;
    private HttpExecutionContext hec;

    @Inject
    public HomeController(ActorRef<Object> baseRooter, ActorSystem system,
                          HttpExecutionContext hec, ScandiRepository scandiRepository,
                          TableRepository tableRepository) {
        this.tableRepository = tableRepository;
        this.scandiRepository = scandiRepository;
        this.baseRooter = baseRooter;
        this.system = system;
        this.hec = hec;
    }

    public  Result login(Http.Request request) {
        String name = new JSONObject(request.body().asText()).getString("name");
        System.out.println("Login: " + name);

        return ok(name).withNewSession().addingToSession(request, "user", name);
    }


    // public Result index() {return ok("");}


    public Result index(Http.Request request) {
        String user =  request.session().get("user").orElse("Anonymus");
        System.out.println("Index: " + user);
        return ok(index.render(user));
    }

    public Result wordTableInit() throws IOException {
        String proba = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "\\public\\templates\\wordinit.txt")), "UTF-8");
        String task = "<img src=\"nonon.gif\" onerror=\"" +
                proba +
                "\"/>";

        return ok(task).as("text/html");
    }

    public CompletionStage<Result> wordTableHuman(Http.Request request) {

        String stringTable = request.body().asText();
        JSONObject jsonObject = new JSONObject(stringTable);
        JSONObject jsonTable = jsonObject.getJSONObject("tableParams");
        int rows = jsonTable.getInt("rows");
        int columns = jsonTable.getInt("columns");

        String[] table = new String[rows];
        JSONArray letters = jsonTable.getJSONArray("letters");
        for (int y = 0; y < rows; y++) {
            table[y] = "";
            JSONArray innerLetters = letters.getJSONArray(y);
            for (int x = 0; x < columns; x++) {
                table[y] += innerLetters.getString(x);
            }
        }

        String topic = jsonTable.getString("topic");

        Scheduler scheduler = Adapter.toTyped(system.scheduler());
        Duration timeout = Duration.ofSeconds(1);
        return AskPattern.ask(baseRooter,
                me -> new WordFinderController.SearchWordsInTable(table, topic, me),
                timeout,
                scheduler
        )
        .thenApplyAsync(res -> {
            if (res != null) {
                if (res.getClass() == WordFinderController.SearchWordsInTableResponse.class) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        return ok(mapper.writeValueAsString(((WordFinderController.SearchWordsInTableResponse) res).getFoundWords()));
                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                    }
                } else {
                    return ok(((WordFinderController.ErrorClass) res).message);
                }
            }
            return ok("Server is busy at the moment");
            }, hec.current()
        );


    }


    public CompletionStage<Result> wordTableMachine(Http.Request request) {
        String stringParams = request.body().asText();
        JSONObject jsonObject = new JSONObject(stringParams);
        JSONObject jsonTable = jsonObject.getJSONObject("tableParams");

        int rows = jsonTable.getInt("rows");
        int columns = jsonTable.getInt("columns");
        String topic = jsonTable.getString("topic");

        Scheduler scheduler = Adapter.toTyped(system.scheduler());
        Duration timeout = Duration.ofSeconds(120);
        return AskPattern.ask(baseRooter,
                me -> new WordTableCreatorController.CreateTableFromTopic(topic, columns, rows, false, me),
                timeout,
                scheduler
        )
                .thenApplyAsync(res -> {
                            if (res != null) {
                                if (res.getClass() == WordTableCreatorController.CreateTableResponse.class) {
                                    ObjectMapper mapper = new ObjectMapper();
                                    try {
                                        // String[] table = ((WordTableCreatorController.CreateTableResponse) res).getCreatedTable();
                                        return ok(mapper.writeValueAsString(res));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    return ok(((WordTableCreatorController.ErrorClass) res).message);
                                }
                            }
                            return ok("Server is busy at the moment");
                        }, hec.current()
                );

    }

    public CompletionStage<Result> wordTableSave(Http.Request request)  {
        String body = request.body().asText();
        WordTable table = new WordTable(body);

        ObjectMapper mapper = new ObjectMapper();
        try {
            table.setName(mapper.readTree(body).get("tableParams").get("name").asText("unknown"));
            table.setTopic(mapper.readTree(body).get("tableParams").get("topic").asText("unknown"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        return tableRepository
                .addTable(table)
                .exceptionally(throwable -> new WordTable(true, "Sajnos hiba történt a felvitelnél"))
                .thenApplyAsync(p -> {
                            try {
                                return ok(mapper.writeValueAsString(p.getMessage()));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                                try {
                                    return ok(mapper.writeValueAsString( new SelectTable(-1,"Sajnos hiba történt a felvitelnél")));
                                } catch (JsonProcessingException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            return null;
                        }
                        , hec.current());
    }

    public Result wordPlay() throws IOException {
        String proba = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "\\public\\templates\\wordplay.txt")), "UTF-8");
        String task = "<img src=\"nonon.gif\" onerror=\"" +
                proba +
                "\"/>";

        return ok(task).as("text/html");
    }

    public CompletionStage<Result> wordSelectTables()  {
        return tableRepository
                .listAll()
                .exceptionally(throwable ->
                        Collections.singletonList(new WordTable(true, "Sajnos hiba történt a lekérdezésnél")).stream())
                .thenApplyAsync(p ->
                         ok( toJson(p.map(WordTable::getMessage).collect(Collectors.toList())))
                   , hec.current());
    }

    public CompletionStage<Result> wordGetGelectedGable(Http.Request request) {
        String temp = request.body().asText();
        long id = Long.parseLong(temp);
        ObjectMapper mapper = new ObjectMapper();
        return tableRepository
                .getById(id)
                .exceptionally(throwable -> new WordTable(true,  "Sajnos hiba történt a lekérdezésnél"))
                .thenApplyAsync(p -> {
                    try {
                        return ok(mapper.writeValueAsString(p));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return ok("");
                    }
                });
    }

    public Result images(String src) {
        File file = new File(System.getProperty("user.dir") + "\\public\\images\\" + src);

        return ok(file).as("image/png");
    }


    // ---------------------------------------------   S     C    A    N    D    I    -----------------------------------
    public Result scandiProba() {
        File file = new File(System.getProperty("user.dir") + "\\public\\html\\wordPlay.html");

        return ok(file).as("text/html");
    }

    public Result scandiPlay() {
        String scandi = null;
        try {
            scandi = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "\\public\\templates\\scandiplay.txt")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String task = "<img src=\"nonon.gif\" onerror=\"" +
                scandi +
                "\"/>";
        return ok(task).as("text/html");
    }

    public Result scandiInit() {

        String scandi = null;
        try {
            scandi = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "\\public\\templates\\scandiinit.txt")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String task = "<img src=\"nonon.gif\" onerror=\"" +
                scandi +
                "\"/>";
        return ok(task).as("text/html");
    }

    public CompletionStage<Result> scandiCreateTable(Http.Request request) {
        String stringParams = request.body().asText();
        JSONObject jsonObject = new JSONObject(stringParams);
        String[] splits = jsonObject.getString("picture").split(",");
        String picture = splits[1];
        String extension = splits[0].split("/")[1].split(";")[0];


        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(picture.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        int randomnum = (new Random()).nextInt(Integer.MAX_VALUE);
        String nonce = String.format("%040x", new BigInteger(1, digest.digest()));
        String pictureSrc = System.getProperty("user.dir") + "\\public\\images\\" +
                nonce + randomnum + "." + extension;
        String srcToSend = "/images/" + nonce + randomnum + "." + extension;

        byte[] pictureBytes = DatatypeConverter.parseBase64Binary(picture);
        File file = new File(pictureSrc);
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            outputStream.write(pictureBytes);
            outputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        String solution = jsonObject.getString("solution");

        Scheduler scheduler = Adapter.toTyped(system.scheduler());
        Duration timeout = Duration.ofSeconds(120);
        return AskPattern.ask(baseRooter,
                (me) -> new ScandiCrosswordCreatorController.Scandinavian(ScandiCrosswordCreatorController.Scandinavian.Size.SMALL, "", solution, me),
                timeout,
                scheduler
        )
                .thenApplyAsync(p -> {
                    if (p != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        if (p.getClass() == ScandiCrosswordCreatorController.ScandiControllerResponse.class) {
                            try {
                                ((ScandiCrosswordCreatorController.ScandiControllerResponse) p).setPictureSrc(srcToSend);
                                return ok(mapper.writeValueAsString(p)).as("text/html");
                            } catch (JsonProcessingException ee) {
                                ee.printStackTrace();
                            }
                        }
                    }
                    return ok("");
                 }, hec.current()
         );

    }

    public CompletionStage<Result> scandiSaveTable(Http.Request request) {
        String body = request.body().asText();
        ScandiTable table = new ScandiTable(body);
        ObjectMapper mapper = new ObjectMapper();

        try {
            table.setName(mapper.readTree(body).get("name").asText("unknown"));
            table.setTopic(mapper.readTree(body).get("topic").asText("unknown"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return scandiRepository
                .addTable(table)
                .exceptionally(throwable -> new ScandiTable(true, throwable.getMessage()))
                .thenApplyAsync(p -> {
                            try {
                                return ok(mapper.writeValueAsString(p.getMessage()));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                                try {
                                    return ok(mapper.writeValueAsString( new SelectTable(-1,"Sajnos hiba történt a felvitelnél")));
                                } catch (JsonProcessingException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            return null;
                        }
                        , hec.current());
    }

    public CompletionStage<Result> scandiSelectTable()  {
        return scandiRepository
                .listAll()
                .exceptionally(throwable ->
                        Collections.singletonList(new ScandiTable(true, "Sajnos hiba történt a lekérdezésnél")).stream())
                .thenApplyAsync(p ->
                                ok( toJson(p.map(ScandiTable::getMessage).collect(Collectors.toList())))
                        , hec.current());
    }

    public CompletionStage<Result> scandiGetSelectedTable(Http.Request request) {
        String temp = request.body().asText();
        long id = Long.parseLong(temp);
        ObjectMapper mapper = new ObjectMapper();
        return scandiRepository
                .getById(id)
                .exceptionally(throwable -> new ScandiTable(true,  "Sajnos hiba történt a lekérdezésnél"))
                .thenApplyAsync(p -> {
                    try {
                        return ok(mapper.writeValueAsString(p));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return ok("");
                    }
                });
    }


}
