import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { PlaybookPage } from './playbook.page';
import { JudgeLinkModalComponent } from './judge-link-modal.component';
import { AdminBackendService } from '../services/admin-backend.service';
import { SecretService } from '../services/secret.service';

const routes: Routes = [{ path: '', component: PlaybookPage }];

@NgModule({
  declarations: [PlaybookPage, JudgeLinkModalComponent],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)],
  providers: [AdminBackendService, SecretService]
})
export class PlaybookPageModule {}
