import { Component, OnInit, ViewChild, HostListener } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf } from '../backend-types';
import { NavController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-last-results',
  templateUrl: './last-results.page.html',
  styleUrls: ['./last-results.page.scss'],
})
export class LastResultsPage implements OnInit {

  // @ViewChild(IonContent) content: IonContent;

  items: WertungContainer[] = [];
  lastItems: number[];
  geraete: Geraet[];

  // @HostListener('window:resize', ['$event'])
  // onResize(event: any) {
  //   if (this.content) {
  //     this.content.resize();
  //   }
  // }

  constructor(public navCtrl: NavController, public backendService: BackendService) {
    this.backendService.getCompetitions();
  }

  ngOnInit(): void {
    this.backendService.loadAlleResultate().subscribe(geraete => {
      this.geraete = geraete;
      this.sortItems();
    });
    this.backendService.newLastResults.pipe(
      filter(r => !!r && !!r.results)
    ).subscribe(newLastRes => {
      this.lastItems = this.items.map(item => item.id * this.geraete.length + item.geraet);
      this.items = [];
      Object.keys(newLastRes.results).forEach(key => {
        this.items.push(newLastRes.results[key]);
      });
      this.sortItems();
    });
  }
  sortItems() {
    this.items = this.items.sort((a, b) => {
      let p = a.programm.localeCompare(b.programm);
      if (p === 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    }).filter(w => w.wertung.endnote !== undefined);
    // if (this.content) {
    //   this.content.resize();
    // }

  }
  isNew(item: WertungContainer): boolean {
    return this.lastItems.filter(id => id === item.id * this.geraete.length + item.geraet).length === 0;
  }
  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }
  set competition(competitionId: string) {
    if (!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.loadAlleResultate().subscribe(geraete => {
        this.geraete = geraete;
        this.sortItems();
      });
    }
  }
  get competition(): string {
    return this.backendService.competition || '';
  }
  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions || [];
  }
  competitionName(): string {
    if (!this.backendService.competitions) { return ''; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  geraetOrder(geraetId: number): number {
    if (!this.geraete) {
      return 0;
    }
    const ret = this.geraete
      .findIndex(c => c.id === geraetId);
    return ret;
  }

  geraetText(geraetId: number): string {
    if (!this.geraete) { return ''; }
    const candidate = this.geraete
      .filter(c => c.id === geraetId)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  getTitle(wertungContainer: WertungContainer): string {
    return wertungContainer.programm + ' - ' + this.geraetText(wertungContainer.geraet);
  }

  itemTapped(event: any, item: WertungContainer) {
    // That's right, we're pushing to ourselves!
    // this.navCtrl.navigateForward('LastResultDetailPage', {
    //   item
    // });
  }
}
