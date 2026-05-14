package com.marsemlixo.api.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.locationtech.jts.geom.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class JtsGeometryModule {

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Bean
    public SimpleModule jtsModule() {
        SimpleModule module = new SimpleModule("JtsGeometryModule");
        module.addSerializer(Polygon.class, new PolygonSerializer());
        module.addDeserializer(Polygon.class, new PolygonDeserializer());
        module.addSerializer(Point.class, new PointSerializer());
        module.addDeserializer(Point.class, new PointDeserializer());
        return module;
    }

    static class PolygonSerializer extends StdSerializer<Polygon> {
        PolygonSerializer() { super(Polygon.class); }

        @Override
        public void serialize(Polygon polygon, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", "Polygon");
            gen.writeArrayFieldStart("coordinates");
            writeRing(gen, polygon.getExteriorRing().getCoordinates());
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                writeRing(gen, polygon.getInteriorRingN(i).getCoordinates());
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }

        private void writeRing(JsonGenerator gen, Coordinate[] coords) throws IOException {
            gen.writeStartArray();
            for (Coordinate c : coords) {
                gen.writeStartArray();
                gen.writeNumber(c.x);
                gen.writeNumber(c.y);
                gen.writeEndArray();
            }
            gen.writeEndArray();
        }
    }

    static class PolygonDeserializer extends StdDeserializer<Polygon> {
        PolygonDeserializer() { super(Polygon.class); }

        @Override
        public Polygon deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                ctx.reportInputMismatch(Polygon.class, "esperado objeto GeoJSON");
            }

            String type = null;
            List<List<double[]>> rings = null;

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();
                if ("type".equals(field)) {
                    type = p.getText();
                } else if ("coordinates".equals(field)) {
                    rings = parseRings(p);
                } else {
                    p.skipChildren();
                }
            }

            if (!"Polygon".equals(type)) {
                ctx.reportInputMismatch(Polygon.class, "type deve ser 'Polygon', recebido: " + type);
            }
            if (rings == null || rings.isEmpty()) {
                ctx.reportInputMismatch(Polygon.class, "coordinates ausentes ou vazias");
            }

            LinearRing exterior = toLinearRing(rings.get(0));
            LinearRing[] holes = new LinearRing[rings.size() - 1];
            for (int i = 1; i < rings.size(); i++) {
                holes[i - 1] = toLinearRing(rings.get(i));
            }
            return FACTORY.createPolygon(exterior, holes);
        }

        private List<List<double[]>> parseRings(JsonParser p) throws IOException {
            List<List<double[]>> rings = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                // START_ARRAY do anel
                List<double[]> ring = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    // START_ARRAY do ponto [longitude, latitude]
                    p.nextToken();
                    double x = p.getDoubleValue();
                    p.nextToken();
                    double y = p.getDoubleValue();
                    p.nextToken(); // END_ARRAY do ponto
                    ring.add(new double[]{x, y});
                }
                rings.add(ring);
            }
            return rings;
        }

        private LinearRing toLinearRing(List<double[]> points) {
            Coordinate[] coords = points.stream()
                    .map(p -> new Coordinate(p[0], p[1]))
                    .toArray(Coordinate[]::new);
            return FACTORY.createLinearRing(coords);
        }
    }

    static class PointSerializer extends StdSerializer<Point> {
        PointSerializer() { super(Point.class); }

        @Override
        public void serialize(Point point, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", "Point");
            gen.writeArrayFieldStart("coordinates");
            gen.writeNumber(point.getX());
            gen.writeNumber(point.getY());
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    static class PointDeserializer extends StdDeserializer<Point> {
        PointDeserializer() { super(Point.class); }

        @Override
        public Point deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                ctx.reportInputMismatch(Point.class, "esperado objeto GeoJSON");
            }

            String type = null;
            double[] coordinates = null;

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();
                if ("type".equals(field)) {
                    type = p.getText();
                } else if ("coordinates".equals(field)) {
                    coordinates = parseCoordinates(p, ctx);
                } else {
                    p.skipChildren();
                }
            }

            if (!"Point".equals(type)) {
                ctx.reportInputMismatch(Point.class, "type deve ser 'Point', recebido: " + type);
            }
            if (coordinates == null) {
                ctx.reportInputMismatch(Point.class, "coordinates ausentes ou inválidas");
            }

            return FACTORY.createPoint(new Coordinate(coordinates[0], coordinates[1]));
        }

        private double[] parseCoordinates(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken() != JsonToken.START_ARRAY) {
                ctx.reportInputMismatch(Point.class, "coordinates deve ser um array [longitude, latitude]");
            }

            if (p.nextToken() != JsonToken.VALUE_NUMBER_FLOAT && p.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                ctx.reportInputMismatch(Point.class, "longitude inválida");
            }
            double longitude = p.getDoubleValue();

            if (p.nextToken() != JsonToken.VALUE_NUMBER_FLOAT && p.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                ctx.reportInputMismatch(Point.class, "latitude inválida");
            }
            double latitude = p.getDoubleValue();

            if (p.nextToken() != JsonToken.END_ARRAY) {
                ctx.reportInputMismatch(Point.class, "coordinates deve conter exatamente dois números");
            }

            return new double[]{longitude, latitude};
        }
    }
}
