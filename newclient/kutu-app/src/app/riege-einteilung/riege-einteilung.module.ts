import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { RiegeEinteilungPage } from './riege-einteilung.page';
import { RiegeEditModalComponent } from '../editors/riege-edit-modal.component';

const routes: Routes = [{ path: '', component: RiegeEinteilungPage }];

@NgModule({
  declarations: [RiegeEinteilungPage, RiegeEditModalComponent],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class RiegeEinteilungPageModule {}
