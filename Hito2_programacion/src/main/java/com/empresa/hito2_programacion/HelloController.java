package com.empresa.hito2_programacion;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.ConnectionString;

import java.util.regex.Pattern;

public class HelloController {

    @FXML
    private TextField textField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField unitsField;

    @FXML
    private ListView<String> addedItemsListView;

    @FXML
    private Label resultLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private TextField searchField;

    private double totalPrice = 0.0;

    private MongoCollection<Document> collection;
    private MongoCollection<Document> counterCollection;

    public void initialize() {
        initMongoDB();
    }

    private void initMongoDB() {
        try {
            MongoClient mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://hito:hito@cluster2.qvwaoqt.mongodb.net/"));
            MongoDatabase database = mongoClient.getDatabase("hito");
            collection = database.getCollection("tienda");
            counterCollection = database.getCollection("counters");

            // Ensure the counter document for productId exists
            Document counter = counterCollection.find(new Document("_id", "productId")).first();
            if (counter == null) {
                counterCollection.insertOne(new Document("_id", "productId").append("seq", 0));
            }

            ObservableList<String> documents = FXCollections.observableArrayList();
            MongoCursor<Document> cursor = collection.find().iterator();
            try {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    documents.add(formatDocument(doc));
                }
            } finally {
                cursor.close();
            }
            addedItemsListView.setItems(documents);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error al conectar con la base de datos MongoDB.");
        }
    }

    private int getNextId(String counterName) {
        Document filter = new Document("_id", counterName);
        Document update = new Document("$inc", new Document("seq", 1));
        Document updatedCounter = counterCollection.findOneAndUpdate(filter, update);
        return updatedCounter.getInteger("seq");
    }

    @FXML
    private void addButtonAction(ActionEvent event) {
        String text = textField.getText();
        String price = priceField.getText();
        String units = unitsField.getText();

        if (!text.isEmpty() && !price.isEmpty() && !units.isEmpty()) {
            try {
                double priceValue = Double.parseDouble(price);
                int unitsValue = Integer.parseInt(units);
                double itemTotal = priceValue * unitsValue;

                int nextId = getNextId("productId"); // Obtener el próximo ID

                Document product = new Document("id", nextId)
                        .append("nombre", text)
                        .append("precio", priceValue)
                        .append("unidades", unitsValue);
                collection.insertOne(product);

                addedItemsListView.getItems().add(formatDocument(product));
                resultLabel.setText("Producto añadido: " + text);

                totalPrice += itemTotal;
                updateTotalLabel();

                textField.clear();
                priceField.clear();
                unitsField.clear();
            } catch (NumberFormatException e) {
                showErrorAlert("Por favor, introduzca un precio y unidades válidos.");
            }
        } else {
            showErrorAlert("Por favor, complete todos los campos.");
        }
    }

    @FXML
    private void deleteButtonAction(ActionEvent event) {
        String selectedItem = addedItemsListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {

                String[] parts = selectedItem.split(": ");
                int id = Integer.parseInt(parts[0]);

                Document query = new Document("id", id);
                collection.deleteOne(query);

                Thread.sleep(100);

                addedItemsListView.getItems().remove(selectedItem);

                double price = Double.parseDouble(parts[1].split(" - ")[1].split(": ")[1]);
                int units = Integer.parseInt(parts[1].split(" - ")[2].split(": ")[1]);
                double itemTotal = price * units;

                totalPrice -= itemTotal;
                updateTotalLabel();

                resultLabel.setText("Producto eliminado: " + selectedItem);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Error al eliminar el producto: " + e.getMessage());
            }
        } else {
            showWarningAlert("Por favor, seleccione un producto para eliminar.");
        }
    }

    @FXML
    private void modifyButtonAction(ActionEvent event) {
        String selectedItem = addedItemsListView.getSelectionModel().getSelectedItem();
        String newText = textField.getText();
        String newPrice = priceField.getText();
        String newUnits = unitsField.getText();

        if (selectedItem != null && !newText.isEmpty() && !newPrice.isEmpty() && !newUnits.isEmpty()) {
            try {
                // Parse the selected item to extract the ID
                String[] parts = selectedItem.split(": ");
                int id = Integer.parseInt(parts[0]);

                double newPriceValue = Double.parseDouble(newPrice);
                int newUnitsValue = Integer.parseInt(newUnits);

                // Check if the price and units are valid
                if (newPriceValue <= 0 || newUnitsValue <= 0) {
                    showErrorAlert("Por favor, introduzca un precio y unidades válidos.");
                    return;
                }

                // Update the product in the database
                Document query = new Document("id", id);
                Document update = new Document("$set", new Document("nombre", newText)
                        .append("precio", newPriceValue)
                        .append("unidades", newUnitsValue));
                collection.updateOne(query, update);

                int selectedIndex = addedItemsListView.getSelectionModel().getSelectedIndex();
                String updatedItem = String.format("%d: %s - Precio: %.2f - Unidades: %d", id, newText, newPriceValue, newUnitsValue);
                addedItemsListView.getItems().set(selectedIndex, updatedItem);

                // Clear the input fields
                textField.clear();
                priceField.clear();
                unitsField.clear();

                resultLabel.setText("Producto modificado: " + newText);

            } catch (NumberFormatException e) {
                showErrorAlert("Por favor, introduzca un precio y unidades válidos.");
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Error al modificar el producto: " + e.getMessage());
            }
        } else {
            showWarningAlert("Por favor, seleccione un producto y complete todos los campos para modificar.");
        }
    }

    @FXML
    private void viewAllButtonAction(ActionEvent event) {
        try {
            ObservableList<String> documents = FXCollections.observableArrayList();
            MongoCursor<Document> cursor = collection.find().iterator();
            try {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    documents.add(formatDocument(doc));
                }
            } finally {
                cursor.close();
            }
            addedItemsListView.setItems(documents);
            resultLabel.setText("Todos los productos cargados.");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error al cargar los productos: " + e.getMessage());
        }
    }


    @FXML
    private void selectButtonAction(ActionEvent event) {
        try {
            ObservableList<String> documents = FXCollections.observableArrayList();
            MongoCursor<Document> cursor = collection.find().iterator();
            try {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    documents.add(formatDocument(doc));
                }
            } finally {
                cursor.close();
            }
            addedItemsListView.setItems(documents);
            resultLabel.setText("Datos seleccionados de la base de datos.");
        } catch (Exception e) {
            e.printStackTrace();
            resultLabel.setText("Error al seleccionar datos de la base de datos: " + e.getMessage());
        }
    }

    private String formatDocument(Document doc) {
        int id = doc.getInteger("id");
        String nombre = doc.getString("nombre");
        double precio = doc.getDouble("precio");
        int unidades = doc.getInteger("unidades");
        return String.format("%d: %s - Precio: %.2f - Unidades: %d", id, nombre, precio, unidades);
    }

    private void updateTotalLabel() {
        totalLabel.setText("Total: " + totalPrice);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Advertencia");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void searchButtonAction(ActionEvent event) {
        String searchTerm = searchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            try {
                ObservableList<String> documents = FXCollections.observableArrayList();
                Document query = new Document("nombre", Pattern.compile(searchTerm, Pattern.CASE_INSENSITIVE));
                MongoCursor<Document> cursor = collection.find(query).iterator();
                try {
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        documents.add(formatDocument(doc));
                    }
                } finally {
                    cursor.close();
                }
                addedItemsListView.setItems(documents);
                resultLabel.setText("Resultados de búsqueda para: " + searchTerm);
            } catch (Exception e) {
                e.printStackTrace();
                resultLabel.setText("Error al realizar la búsqueda: " + e.getMessage());
            }
        } else {
            showWarningAlert("Por favor, ingrese un término de búsqueda.");
        }
    }
}
