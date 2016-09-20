import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import hex.genmodel.MojoModel;
import hex.genmodel.MojoModel.MojoReader;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.exception.PredictException;
import hex.genmodel.easy.prediction.RegressionModelPrediction;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.HashMap;
import java.util.zip.*;

public class App {

    static class InputStreamMojoReader implements MojoReader {
        private HashMap<String,byte[]> files;

        public InputStreamMojoReader(InputStream stream) throws IOException {
            this.files = parseZipInputStream(new ZipInputStream(stream));
        }

        private HashMap<String, byte[]> parseZipInputStream(ZipInputStream zis) throws IOException {
            HashMap<String, byte[]> map = new HashMap<String, byte[]>();
            ZipEntry entry;
            ByteArrayOutputStream baos;

            while((entry = zis.getNextEntry()) != null) {
                int size;
                byte[] buffer = new byte[2048];
                baos = new ByteArrayOutputStream();
                while((size = zis.read(buffer, 0, buffer.length)) != -1) {
                    baos.write(buffer, 0, size);
                }
                map.put(entry.getName(), baos.toByteArray());
            }

            return map;
        }

        @Override
        public BufferedReader getTextFile(String filename) throws IOException {
            ByteArrayInputStream input = new ByteArrayInputStream(this.files.get(filename));
            return new BufferedReader(new InputStreamReader(input));
        }

        @Override
        public byte[] getBinaryFile(String filename) throws IOException {
            byte[] file = this.files.get(filename);
            if (file == null)
                throw new IOException("Tree file " + filename + " not found");
            return file;
        }
    }

    public static void main (String[] args) throws IOException {
        // connect to Mongo
        MongoClient client = new MongoClient("localhost", 27017);
        MongoDatabase db = client.getDatabase("test");
        MongoCollection<Document> models = db.getCollection("models");

        // grab model
        Document myModel = models.find().first();
        System.out.println(myModel);

        // decode model
        byte[] modelByteStream = DatatypeConverter.parseBase64Binary(myModel.get("file").toString());
        EasyPredictModelWrapper modelFromStream = new EasyPredictModelWrapper(MojoModel.load(new InputStreamMojoReader(new ByteArrayInputStream(modelByteStream))));

        EasyPredictModelWrapper modelFromFile = new EasyPredictModelWrapper(MojoModel.load("/Users/nkkarpov/MyModelz.zip"));

        RowData row = new RowData();
        row.put("ID","1");
        row.put("CAPSULE","0");
        row.put("AGE","65");
        row.put("RACE","1");
        row.put("DPROS","2");
        row.put("DCAPS","1");
        row.put("PSA","1.4");
        row.put("VOL","0.0");

        RegressionModelPrediction p;
        RegressionModelPrediction p2;

        try {
            p = modelFromFile.predictRegression(row);
            p2 = modelFromStream.predictRegression(row);

            System.out.println(p);
            System.out.println(p2);
        } catch (PredictException pe) {}
    }
}
