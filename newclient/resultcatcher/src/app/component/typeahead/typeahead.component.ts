import { Component, Input, Output, EventEmitter } from '@angular/core';
import type { OnInit } from '@angular/core';

export interface TypeAheadItem<T> {
  item: T;
  text: string;
};
@Component({
    selector: 'app-typeahead',
    templateUrl: 'typeahead.component.html',
    standalone: false
})
export class TypeaheadComponent<T> implements OnInit {
  @Input() items: TypeAheadItem<T>[] = [];
  @Input() selectedItem: T;
  @Input() title = 'Select Items';

  @Output() selectionCancel = new EventEmitter<void>();
  @Output() selectionChange = new EventEmitter<T>();

  filteredItems: TypeAheadItem<T>[] = [];
  workingSelectedValue: TypeAheadItem<T> = undefined;

  ngOnInit() {
    this.filteredItems = [...this.items];
    this.workingSelectedValue = <TypeAheadItem<T>>{
      item: this.selectedItem,
      text: this.items.find(i => i.item === this.selectedItem)?.text
    };
  }

  cancelChanges() {
    this.selectionCancel.emit();
  }

  confirmChanges() {
    this.selectionChange.emit(this.workingSelectedValue?.item);
  }

  searchbarInput(ev) {
    this.filterList(ev.target.value);
  }

  /**
   * Update the rendered view with
   * the provided search query. If no
   * query is provided, all data
   * will be rendered.
   */
  filterList(searchQuery: string | undefined) {
    /**
     * If no search query is defined,
     * return all options.
     */
    if (searchQuery === undefined) {
      this.filteredItems = [...this.items];
    } else {
      /**
       * Otherwise, normalize the search
       * query and check to see which items
       * contain the search query as a substring.
       */
      const normalizedQuery = searchQuery.toLowerCase();
      this.filteredItems = this.items.filter((item) => {
        return item.text.toLowerCase().includes(normalizedQuery);
      });
    }
  }

  get selected() {
    return this.workingSelectedValue;
  }

  set selected(item: TypeAheadItem<T>) {
    this.workingSelectedValue = item;
  }
  
}