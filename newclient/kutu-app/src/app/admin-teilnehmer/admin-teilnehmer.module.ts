import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TeilnehmerEditModalComponent } from './teilnehmer-edit-modal.component';
import { AdminTeilnehmerPage } from './admin-teilnehmer.page';

const routes: Routes = [{ path: '', component: AdminTeilnehmerPage }];

@NgModule({
  declarations: [AdminTeilnehmerPage, TeilnehmerEditModalComponent],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class AdminTeilnehmerPageModule {}
