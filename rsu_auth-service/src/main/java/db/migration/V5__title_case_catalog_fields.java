package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import co.edu.uniajc.rsu_auth_service.service.CatalogTitleNormalizer;

public class V5__title_case_catalog_fields extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        normalizeColumn(context.getConnection(), "espacios", "nombre");
        normalizeColumn(context.getConnection(), "espacios", "ubicacion");
        normalizeColumn(context.getConnection(), "tipos_espacio", "nombre");
    }

    private void normalizeColumn(Connection connection, String table, String column) throws SQLException {
        List<CatalogText> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT id, " + column + " FROM " + table)) {
            while (rows.next()) {
                values.add(new CatalogText(rows.getObject("id"), rows.getString(column)));
            }
        }

        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE " + table + " SET " + column + " = ? WHERE id = ?")) {
            for (CatalogText value : values) {
                String titleCaseValue = CatalogTitleNormalizer.normalize(value.text());
                if (!titleCaseValue.equals(value.text())) {
                    update.setString(1, titleCaseValue);
                    update.setObject(2, value.id());
                    update.addBatch();
                }
            }
            update.executeBatch();
        }
    }

    private record CatalogText(Object id, String text) {
    }
}
