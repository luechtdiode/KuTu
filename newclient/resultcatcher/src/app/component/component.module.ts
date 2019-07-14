import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResultDisplayComponent } from './result-display/result-display.component';
import { RiegeListComponent } from './riege-list/riege-list.component';
import { StartlistItemComponent } from './startlist-item/startlist-item.component';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@NgModule({
  declarations: [ResultDisplayComponent, RiegeListComponent, StartlistItemComponent],
  exports: [ResultDisplayComponent, RiegeListComponent, StartlistItemComponent],
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
  ]
})
export class ComponentsModule { }
