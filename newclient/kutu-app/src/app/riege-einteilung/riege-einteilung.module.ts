import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { RiegeEinteilungPage } from './riege-einteilung.page';

const routes: Routes = [{ path: '', component: RiegeEinteilungPage }];

@NgModule({
  declarations: [RiegeEinteilungPage],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class RiegeEinteilungPageModule {}
