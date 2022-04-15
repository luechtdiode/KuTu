package javafx.scene.control.skin;

import com.sun.javafx.scene.control.behavior.TableCellBehavior;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

@SuppressWarnings("unchecked")
public class TableCellSkin<S, T> extends TableCellSkinBase<S, T, TableCell<S, T>> {
    private final TableCellBehavior behavior;

    public TableCellSkin(TableCell<S, T> var1) {
        super(var1);
        this.behavior = new TableCellBehavior(var1);
    }

    public void dispose() {
        super.dispose();
        if (this.behavior != null) {
            this.behavior.dispose();
        }

    }

    public ReadOnlyObjectProperty<TableColumn<S, T>> tableColumnProperty() {
        TableCell<S, T> skinnable = this.getSkinnable();
        if (skinnable != null) {
            return skinnable.tableColumnProperty();
        } else {
            return new ReadOnlyObjectProperty<>() {
                @Override
                public Object getBean() {
                    return null;
                }

                @Override
                public String getName() {
                    return null;
                }

                @Override
                public TableColumn<S, T> get() {
                    return null;
                }

                @Override
                public void addListener(ChangeListener<? super TableColumn<S, T>> changeListener) {

                }

                @Override
                public void removeListener(ChangeListener<? super TableColumn<S, T>> changeListener) {

                }

                @Override
                public void addListener(InvalidationListener invalidationListener) {

                }

                @Override
                public void removeListener(InvalidationListener invalidationListener) {

                }
            };
        }
    }
}
