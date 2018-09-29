import { Component, ViewChild, HostListener, OnInit } from '@angular/core';
import { NavController, NavParams, Content, Header } from 'ionic-angular';
import { LastResultDetailPage } from '../last-result-detail/last-result-detail';
import { BackendService } from '../../app/backend.service';
import { WertungContainer, Wettkampf, Geraet } from '../../app/backend-types';

/**
 * Generated class for the LastResultsPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@Component({
  selector: 'page-last-results',
  templateUrl: 'last-results.html',
})
export class LastResultsPage implements OnInit {

  @ViewChild(Content) content: Content;
  @ViewChild(Header) header: Header;

  items: WertungContainer[] = [];
  geraete: Geraet[];

  @HostListener('window:resize', ['$event'])
  onResize(event){
    if (this.content) {
      this.content.resize();      
    }
  }

  constructor(public navCtrl: NavController, public navParams: NavParams, public backendService: BackendService) {
  }
  ngOnInit(): void {
    this.backendService.loadAlleResultate().subscribe(geraete => {
      this.geraete = geraete;
      this.sortItems();
    });
    this.backendService.newLastResults
    .filter(r => !!r && !!r.results)
    .subscribe(newLastRes => {
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
      if (p == 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    });
    if (this.content) {
      this.content.resize();      
    }
    
  }

  get stationFreezed(): Boolean {
    return this.backendService.stationFreezed;
  }
  set competition(competitionId: string) {
    if(!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.loadAlleResultate().subscribe(geraete => {
        this.geraete = geraete;
        this.sortItems();
      });
    }
  }
  get competition(): string {
    return this.backendService.competition || "";
  }
  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }
  competitionName(): string {
    if (!this.backendService.competitions) return '';
    let candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join("-"));

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
    if (!this.geraete) return '';
    let candidate = this.geraete
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

  itemTapped(event, item: WertungContainer) {
    // That's right, we're pushing to ourselves!
    this.navCtrl.push(LastResultDetailPage, {
      item: item
    });
  }
}
