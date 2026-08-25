import { Component, EventEmitter, HostBinding, Input, OnInit, Output, input, viewChild, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    selector: 'app-wertung-avg-calc',
    templateUrl: './wertung-avg-calc.component.html',
    styleUrls: ['./wertung-avg-calc.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            multi: true,
            useExisting: WertungAvgCalcComponent
        }
    ],
    standalone: false
})
export class WertungAvgCalcComponent implements ControlValueAccessor { 
  static nextId = 0;

  @HostBinding() id = `avg-calc-input-${WertungAvgCalcComponent.nextId++}`;

  readonly hidden = input<boolean>(undefined);

  readonly readonly = input<boolean>(undefined);
  
  readonly waiting = input<boolean>(undefined);

  readonly valueTitle = input<string>(undefined);

  readonly valueDescription = input<string>(undefined);
  
  _fixed: number;
  
  get fixed(): number {
    this._fixed = Number(localStorage.getItem(`avg-calc-decimals-${this.valueTitle()}${localStorage.getItem('current_competition')}`)) || this._fixed;
    return this._fixed;
  }

  @Input()
  set fixed(value: number) {
    this._fixed = value;
  }

  _compMethod: string = 'avg';
  avgValue = signal<number>(undefined);

  singleValues = signal<{value:number}[]>([]);

  get title(): string {
    return this.valueTitle();
  }

  get methodSymbol(): string {
    if (this._compMethod === 'avg') {
      return 'Ø';
    } else {
      return 'Σ';
    }
  }

  get singleValueContainer() {
    return this.singleValues()[0]?.value || this.avgValue();
  }

  set singleValueContainer(avgValue: number) {
    if (this.avgValue() === avgValue
      && this.singleValues().length === 1
      && this.singleValues()[0]?.value === avgValue) {
      return;
    }
    this.singleValues.set([{value: avgValue}]);
    this.calcAvg();
    this.markAsTouched();
  }

  get compMethod(): string {
    this._compMethod = localStorage.getItem(`comp-method-${this.valueTitle()}${localStorage.getItem('current_competition')}`) || this._compMethod;
    return this._compMethod;
  }
  set compMethod(value: string) {
    if (value === this._compMethod) {
      return;
    }
    this._compMethod = value;
    localStorage.setItem(`comp-method-${this.valueTitle()}${localStorage.getItem('current_competition')}`, value);
    this.calcAvg();
    this.markAsTouched();
  }

  addKomma() {
    if (this._fixed < 3) {
      this._fixed += 1;
      localStorage.setItem(`avg-calc-decimals-${this.valueTitle()}${localStorage.getItem('current_competition')}`, '' + this._fixed);
      this.calcAvg();
      this.markAsTouched();
    }
  }

  removeKomma() {
    if (this._fixed > 0) {
      this._fixed -= 1;
      localStorage.setItem(`avg-calc-decimals-${this.valueTitle()}${localStorage.getItem('current_competition')}`, '' + this._fixed);
      this.calcAvg();
      this.markAsTouched();
    }
  }

  add() {
    if (!this.disabled()) {
      this.singleValues.update(singleValues => [...singleValues, {value: 0.000}]);
      if (this.singleValues().length === 1) {
        this.singleValues.update(singleValues => [...singleValues, {value: 0.000}]);
      }
      this.markAsTouched();
    }
  }

  remove(index) {
    if (!this.disabled() && index > -1) {
      this.singleValues.update(singleValues => singleValues.filter((_, i) => i !== index));
      this.calcAvg();
      this.markAsTouched();
    }
  }

  calcAvg(): number {
    const avg1 = this.singleValues()
      .filter(item => !!item.value)
      .map(item => Number(item.value))
      .filter(item => !isNaN(item))
      .filter(item => item > 0)
    if (avg1.length === 0) {
      this.avgValue.set(undefined);
      this.onChange(undefined);
      //console.log('value updated: ' + undefined);
      return undefined;
    }
    const divider = this.compMethod === 'avg' ? avg1.length : 1;
    const avg2 = Number((avg1.reduce((sum, current) => sum + current, 0) / divider).toFixed(this.fixed));
    if (!this.disabled() && this.avgValue() !== avg2 && !isNaN(avg2)) {
      this.avgValue.set(avg2);
      this.onChange(avg2);
      //console.log('value updated: ' + avg2);
    }
    return avg2;
  }

  onTouched = () => {};
  
  onChange = (avgValue: number) => {};

  onItemChange(event, item) {
    item.value = event.target.value;
    this.calcAvg();
    this.markAsTouched();
  }
  
  onBlur(event, item) {
    item.value = Number(event.target.value).toFixed(this.fixed);
    this.calcAvg();
    this.markAsTouched();
  }

  touched = false;

  disabled = signal(false);

  writeValue(avgValue: number) {
    this.avgValue.set(avgValue);
    this.singleValues.set([{value: avgValue}]);
  }

  registerOnChange(onChange: any) {
    this.onChange = onChange;
  }

  registerOnTouched(onTouched: any) {
    this.onTouched = onTouched;
  }

  markAsTouched() {
    if (!this.touched) {
      this.onTouched();
      this.touched = true;
    }
  }

  setDisabledState(disabled: boolean) {
    this.disabled.set(disabled);
  }

  public readonly noteInput = viewChild<{
    setFocus: () => void;
}>('noteInput');
  
  focused = false;

  setFocus() {
    this.noteInput().setFocus();
  }
}
