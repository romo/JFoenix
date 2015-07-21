package com.cctintl.c3dfx.controls;

import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;

import com.cctintl.c3dfx.concurrency.CFXUtilities;
import com.cctintl.c3dfx.controls.datamodels.treetable.RecursiveTreeObject;

/**
 * @author sshahine
 * RecursiveTreeItem is used along with RecursiveTreeObject
 * to build the data model for the TreeTableView
 *
 * @param <T> 
 */

public class RecursiveTreeItem<T extends RecursiveTreeObject<T>> extends TreeItem<T> {
	
	private Callback<RecursiveTreeObject<T>, ObservableList<T>> childrenFactory;

	private ObjectProperty<Predicate<TreeItem<T>>> predicate = new SimpleObjectProperty<Predicate<TreeItem<T>>>((TreeItem<T> t) -> true);

	ObservableList<TreeItem<T>> originalItems = FXCollections.observableArrayList();
	
	private FilteredList<TreeItem<T>> filteredList ;
	
	/***************************************************************************
	 *                                                                         *
	 * Constructors                                                            *
	 *                                                                         *
	 **************************************************************************/
	
	public RecursiveTreeItem(Callback<RecursiveTreeObject<T>, ObservableList<T>> func) {
		this(null, (Node) null, func);
	}

	public RecursiveTreeItem(final T value, Callback<RecursiveTreeObject<T>, ObservableList<T>> func) {
		this(value, (Node) null, func);
	}

	public RecursiveTreeItem(final T value, Node graphic, Callback<RecursiveTreeObject<T>, ObservableList<T>> func) {
		super(value, graphic);
		this.childrenFactory = func;
		init(value);
	}

	public RecursiveTreeItem(ObservableList<T> dataList, Callback<RecursiveTreeObject<T>, ObservableList<T>> func) {
		RecursiveTreeObject<T> root = new RecursiveTreeObject<>();
		root.getChildren().addAll(dataList);
		
		this.childrenFactory = func;
		init(root);
	}

	private void init(RecursiveTreeObject<T> value){
		
		if (value != null) {
			addChildrenListener(value);
		}
		valueProperty().addListener((o, oldValue, newValue) -> {
			if (newValue != null) {
				addChildrenListener(newValue);
			}
		});
		
		this.filteredList.predicateProperty().bind(Bindings.createObjectBinding(() -> {			
            return child -> {
                // Set the predicate of child items to force filtering
                if (child instanceof RecursiveTreeItem) {
                	RecursiveTreeItem<T> filterableChild = (RecursiveTreeItem<T>) child;
                    filterableChild.setPredicate(this.predicate.get());
                }
                // If there is no predicate, keep this tree item
                if (this.predicate.get() == null)
                    return true;
                // If there are children, keep this tree item
                if (child.getChildren().size() > 0)
                    return true;
                // If its a group node keep this item if it has children
                if (child.getValue() instanceof RecursiveTreeObject && child.getValue().getClass() == RecursiveTreeObject.class){
                	if(child.getChildren().size() == 0)
                		return false;
                	return true;
                }
                // Otherwise ask the TreeItemPredicate
                return this.predicate.get().test(child);
            };
        }, this.predicate));
		
		
		this.filteredList.predicateProperty().addListener((o,oldVal,newVal)->{
			CFXUtilities.runInFXAndWait(()->{
				getChildren().clear();
				getChildren().addAll(filteredList);
			});
		});
	}
	
	private void addChildrenListener(RecursiveTreeObject<T> value) {
		final ObservableList<T> children = childrenFactory.call(value);
		originalItems = FXCollections.observableArrayList();
		for(T child : children)
			originalItems.add(new RecursiveTreeItem<>(child, getGraphic(), childrenFactory));
		
		filteredList = new FilteredList<>(originalItems, (TreeItem<T> t) -> true);
		
		this.getChildren().addAll(originalItems);
		
		children.addListener((ListChangeListener<T>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					change.getAddedSubList().forEach(t -> {
						RecursiveTreeItem<T> newItem = new RecursiveTreeItem<>(t, getGraphic(), childrenFactory);
						RecursiveTreeItem.this.getChildren().add(newItem);
						originalItems.add(newItem);
					});
				}
				if (change.wasRemoved()) {
					change.getRemoved().forEach(t -> {
						for(int i = 0 ; i < RecursiveTreeItem.this.getChildren().size(); i++){
							if(this.getChildren().get(i).getValue().equals(t)){
								this.getChildren().remove(i);
								originalItems.remove(i);
								i--;
							}							
						}
//						final List<TreeItem<T>> itemsToRemove = RecursiveTreeItem.this.getChildren().stream()
//								.filter(treeItem -> treeItem.getValue().equals(t)).collect(Collectors.toList());
//						RecursiveTreeItem.this.getChildren().removeAll(itemsToRemove);
					});
				}
			}
		});
		
	}

	public final ObjectProperty<Predicate<TreeItem<T>>> predicateProperty() {
		return this.predicate;
	}

	public final Predicate<TreeItem<T>> getPredicate() {
		return this.predicateProperty().get();
	}

	public final void setPredicate(final Predicate<TreeItem<T>> predicate) {
		this.predicateProperty().set(predicate);
	}



}