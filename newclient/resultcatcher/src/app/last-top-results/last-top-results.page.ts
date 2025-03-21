import { Component, OnInit } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf } from '../backend-types';
import { BackendService } from '../services/backend.service';
import { filter } from 'rxjs/operators';
import { GroupBy } from '../component/result-display/result-display.component';

@Component({
    selector: 'app-last-top-results',
    templateUrl: './last-top-results.page.html',
    styleUrls: ['./last-top-results.page.scss'],
    standalone: false
})
export class LastTopResultsPage implements OnInit {
  groupBy = GroupBy; 
  items: WertungContainer[] = [];
  toptop = {};
  geraete: Geraet[] = [];

  ngOnInit() {
    this.backendService.competitionSubject.subscribe(comps => {
      this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(geraete => {
        this.geraete = geraete || [];
        this.sortItems();
      });
      this.backendService.newLastResults.pipe(
        filter(r => !!r && !!r.lastTopResults)
      ).subscribe(newLastRes => {
        this.items = [];
        this.toptop = {};
        Object.keys(newLastRes.lastTopResults).forEach(key => {
          const top = newLastRes.lastTopResults[key];
          const topt = this.toptop[top.wertung.wettkampfdisziplinId];
          if (!topt) {
            this.toptop[top.wertung.wettkampfdisziplinId] = top;
          } else if (topt.wertung.endnote < top.wertung.endnote) {
            this.toptop[top.wertung.wettkampfdisziplinId] = top;
          }
        });
        Object.keys(this.toptop).forEach(key => {
          const top = this.toptop[key];
          this.items.push(top);
        });
        this.sortItems();
      });
    });
  }

  constructor(public backendService: BackendService) {
    if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  sortItems() {
    this.items = this.items.sort((a, b) => {
      let p = a.programm.localeCompare(b.programm);
      if (p === 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    });
    // if (this.content) {
    //   this.content.resize();
    // }

  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }
  set competition(competitionId: string) {
    if (!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(geraete => {
        this.geraete = geraete || [];
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

  getColumnSpec(): number {
    return this.geraete?.length || 0;
  }

  getMaxColumnSpec(): number {
    return Math.min(12, Math.max(1, Math.floor(12 / this.geraete.length + 0.5)));
  }
  
  getTitle(wertungContainer: WertungContainer): string {
    return wertungContainer.programm + ' - ' + this.geraetText(wertungContainer.geraet);
  }
  onlyUnique(value, index, self) {
    return self.indexOf(value) === index;
  }
  getProgramme() {
    return this.items.map(wc => wc.programm).filter(this.onlyUnique);
  }

  getWertungen(programm) {
    return this.items.filter(wc => wc.programm === programm);
  }

  itemTapped(event, item) {
  }
}
