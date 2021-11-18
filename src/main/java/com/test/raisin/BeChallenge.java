package com.test.raisin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.test.raisin.vo.Kind;
import com.test.raisin.vo.ResponseA;
import com.test.raisin.vo.SinkResponse;
import okhttp3.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BeChallenge {
    private final OkHttpClient httpClient = new OkHttpClient();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private Gson gson = new Gson();
    private ConcurrentMap<String, String> setA = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> setB = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        BeChallenge read = new BeChallenge();
        read.getResources();
    }

    private void getResources() {
        CompletableFuture sourceA = CompletableFuture.runAsync(() -> {
            getSourceAData();
        });
        CompletableFuture sourceB = CompletableFuture.runAsync(() -> {
            getSourceBData();
        });

        while (!sourceA.isDone() || !sourceB.isDone())
            this.compareJoin();
        this.compare();
    }

    private boolean getSourceAData() {
        boolean isDoneA = false;
        Request requestA = new Request.Builder()
                .url("http://localhost:7299/source/a")
                .build();
        do {
            try (Response response = httpClient.newCall(requestA).execute()) {
                if (response.code() == 406)
                    getSourceBData();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);
                if (response.body() != null) {
                    String result = response.body().string();
                    Gson gson = new Gson();
                    ResponseA responseA = gson.fromJson(result, ResponseA.class);
                    isDoneA = responseA.getStatus().equalsIgnoreCase("done") ? true : false;
                    if (!isDoneA) {
                        setA.put(responseA.getId(), "");
                    }
                }
            } catch (JsonSyntaxException e) {
                System.out.println(" Invalid JSON ");
            } catch (IOException e) {
                System.out.println(" Exception while connecting to source A : " + e.getMessage());
            }
        } while (!isDoneA);
        return true;
    }

    private boolean getSourceBData() {
        Request requestB = new Request.Builder()
                .url("http://localhost:7299/source/b")
                .build();
        boolean isDoneB = false;
        do {
            try (Response response = httpClient.newCall(requestB).execute()) {
                if (response.code() == 406)
                    getSourceAData();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);
                if (response.body() != null) {
                    String result = response.body().string();
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    ByteArrayInputStream input = new ByteArrayInputStream(new StringBuilder().append(result).toString().getBytes("UTF-8"));
                    Document document = db.parse(input);
                    document.getDocumentElement().normalize();
                    NodeList nDone = document.getElementsByTagName("done");
                    Node done = nDone.item(0);
                    if (done == null) {
                        NodeList nList = document.getElementsByTagName("id");
                        Node node = nList.item(0);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) node;
                            setB.put(eElement.getAttribute("value"), "");
                        }
                    } else {
                        isDoneB = true;
                    }
                }
            } catch (IOException e) {
                System.out.println(" Exception while connecting to source B : " + e.getMessage());
            } catch (ParserConfigurationException e) {
                System.out.println(" Invalid XML ");
            } catch (SAXException e) {
                System.out.println(" Invalid XML ");
            }
        } while (!isDoneB);
        return true;
    }

    private void compareJoin() {
        Iterator<String> itA = setA.keySet().iterator();
        while (itA.hasNext()) {
            String idA = itA.next();
            if (setB.containsKey(idA)) {
                SinkResponse res = new SinkResponse();
                res.setKind(Kind.JOINED.kind);
                res.setId(idA);
                setA.remove(idA);
                setB.remove(idA);
                sendSink(gson.toJson(res));
            }
        }
    }

    private void compare() {
        compareJoin();
        Iterator<String> itA = setA.keySet().iterator();
        Iterator<String> itB = setB.keySet().iterator();

        while (itA.hasNext()) {
            String idA = itA.next();
            SinkResponse res = new SinkResponse();
            res.setKind(Kind.ORPHANED.kind);
            res.setId(idA);
            sendSink(gson.toJson(res));
        }

        while (itB.hasNext()) {
            String idB = itB.next();
            SinkResponse res = new SinkResponse();
            res.setId(idB);
            res.setKind(Kind.ORPHANED.kind);
            sendSink(gson.toJson(res));
        }
    }

    private void sendSink(String sinkResp) {
        RequestBody body = RequestBody.create(sinkResp, JSON);
        Request request = new Request.Builder()
                .url("http://localhost:7299/sink/a")
                .post(body)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
