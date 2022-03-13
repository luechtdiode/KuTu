import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResultDisplayComponent } from './result-display/result-display.component';
import { RiegeListComponent } from './riege-list/riege-list.component';
import { StartlistItemComponent } from './startlist-item/startlist-item.component';
import { ScorelistItemComponent } from './scorelist-item/scorelist-item.component';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@NgModule({
  declarations: [ResultDisplayComponent, RiegeListComponent, StartlistItemComponent, ScorelistItemComponent],
  exports: [ResultDisplayComponent, RiegeListComponent, StartlistItemComponent, ScorelistItemComponent],
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
  ]
})
export class ComponentsModule { }
