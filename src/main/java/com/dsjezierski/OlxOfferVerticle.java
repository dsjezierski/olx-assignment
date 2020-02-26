package com.dsjezierski;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;

public class OlxOfferVerticle extends AbstractVerticle {


    @Override
    public void start(Future<Void> fut) {
        Router router = Router.router(vertx);
        createRoutes(router);
        createServer(fut, router);

    }

    private void createRoutes(Router router) {
        router.get("/offers/olx").handler(this::getOffers);
    }

    private void createServer(Future<Void> fut, Router router) {
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }

    private List<Offer> parseHtml(String html){
        Document doc = Jsoup.parse(html);
        List<Offer> offers = new LinkedList<>();

        Elements offerElements = doc.getElementsByClass("offer-wrapper");

        for (Element element : offerElements){
            Elements titleElement = element.getElementsByClass("title-cell");
            Elements priceElement = element.getElementsByClass("space inlblk rel");
            try{
                String id = element.getElementsByTag("table").get(0).attributes().get("data-id");
                String title = titleElement.get(0).getElementsByTag("strong").get(0).childNode(0).toString();
                String price = priceElement.get(0).getElementsByTag("strong").get(0).childNode(0).toString();

                offers.add(new Offer(id, title, price));
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        return offers;
    }

    private void getOffers(RoutingContext routingContext) {
        String keyword = routingContext.request().getParam("keyword");
        HttpRequest<String> request = WebClient.create(vertx)
                .get(443, "olx.pl", "/oferty/q-" + keyword)
                .ssl(true)  // (3)
                .putHeader("Accept", "application/json")
                .as(BodyCodec.string())
                .expect(ResponsePredicate.SC_OK);

        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                List<Offer> offers = parseHtml(asyncResult.result().body());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(offers));
            }
        });
    }



}
