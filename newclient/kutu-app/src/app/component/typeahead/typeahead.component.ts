import { Component, computed, input, output, signal } from '@angular/core';
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
  readonly items = input<TypeAheadItem<T>[]>([]);
  readonly selectedItem = input<T>(undefined);
  readonly title = input('Select Items');

  readonly selectionCancel = output<void>();
  readonly selectionChange = output<T>();

  query = signal('');

  workingSelectedValue = signal<TypeAheadItem<T>>(undefined);

  filteredItems = computed(() => {
    const items = this.items() || [];
    const normalizedQuery = this.query()?.toLowerCase();
    if (!normalizedQuery) {
      return [...items];
    }
    return items.filter(item => item.text.toLowerCase().includes(normalizedQuery));
  });

  ngOnInit() {
    this.workingSelectedValue.set(<TypeAheadItem<T>>{
      item: this.selectedItem(),
      text: (this.items() || []).find(i => i.item === this.selectedItem())?.text
    });
  }

  cancelChanges() {
    this.selectionCancel.emit(void 0);
  }

  confirmChanges() {
    this.selectionChange.emit(this.workingSelectedValue()?.item);
  }

  searchbarInput(ev) {
    this.query.set(ev.target.value);
  }

  get selected(): TypeAheadItem<T> {
    return this.workingSelectedValue();
  }

  set selected(item: TypeAheadItem<T>) {
    this.workingSelectedValue.set(item);
  }

}