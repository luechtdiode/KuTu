(self.webpackChunkresultcatcher=self.webpackChunkresultcatcher||[]).push([[9507],{9507:(e,t,i)=>{"use strict";i.r(t),i.d(t,{LastTopResultsPageModule:()=>b});var o=i(8583),n=i(665),s=i(2952),c=i(2859),r=i(5435),a=i(8186),l=i(600),p=i(2997);function g(e,t){if(1&e&&(a.TgZ(0,"ion-select-option",9),a._uU(1),a.ALo(2,"date"),a.qZA()),2&e){const e=t.$implicit;a.Q6J("value",e.uuid),a.xp6(1),a.hij(" ",e.titel+" "+a.xi3(2,2,e.datum,"dd-MM-yy"),"")}}function u(e,t){if(1&e){const e=a.EpF();a.TgZ(0,"ion-select",7),a.NdJ("ngModelChange",function(t){return a.CHM(e),a.oxw(2).competition=t}),a.YNc(1,g,3,5,"ion-select-option",8),a.qZA()}if(2&e){const e=a.oxw(2);a.Q6J("ngModel",e.competition),a.xp6(1),a.Q6J("ngForOf",e.getCompetitions())}}function d(e,t){if(1&e&&(a.TgZ(0,"ion-col"),a.TgZ(1,"ion-item"),a.TgZ(2,"ion-label"),a._uU(3,"Wettkampf"),a.qZA(),a.YNc(4,u,2,2,"ion-select",6),a.qZA(),a.qZA()),2&e){const e=a.oxw();a.xp6(4),a.Q6J("ngIf",e.getCompetitions().length>0)}}function m(e,t){if(1&e&&(a.TgZ(0,"ion-note",10),a._uU(1),a.qZA()),2&e){const e=a.oxw();a.xp6(1),a.hij(" ",e.competitionName()," ")}}function h(e,t){if(1&e){const e=a.EpF();a.TgZ(0,"ion-col",11),a.NdJ("click",function(t){const i=a.CHM(e).$implicit;return a.oxw().itemTapped(t,i)}),a._UZ(1,"result-display",12),a.qZA()}if(2&e){const e=t.$implicit,i=a.oxw();a.xp6(1),a.Q6J("item",e)("title",i.getTitle(e))}}let f=(()=>{class e{constructor(e){this.backendService=e,this.items=[],this.toptop={},this.geraete=[],this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(e=>{this.geraete=e||[],this.sortItems()}),this.backendService.newLastResults.pipe((0,r.h)(e=>!!e&&!!e.lastTopResults)).subscribe(e=>{this.items=[],this.toptop={},Object.keys(e.lastTopResults).forEach(t=>{const i=e.lastTopResults[t],o=this.toptop[i.wertung.wettkampfdisziplinId];o?o.wertung.endnote<i.wertung.endnote&&(this.toptop[i.wertung.wettkampfdisziplinId]=i):this.toptop[i.wertung.wettkampfdisziplinId]=i}),Object.keys(this.toptop).forEach(e=>{this.items.push(this.toptop[e])}),this.sortItems()})}sortItems(){this.items=this.items.sort((e,t)=>{let i=e.programm.localeCompare(t.programm);return 0===i&&(i=this.geraetOrder(e.geraet)-this.geraetOrder(t.geraet)),i})}get stationFreezed(){return this.backendService.stationFreezed}set competition(e){this.stationFreezed||(this.backendService.getDurchgaenge(e),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(e=>{this.geraete=e||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";const e=this.backendService.competitions.filter(e=>e.uuid===this.backendService.competition).map(e=>e.titel+", am "+(e.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===e.length?e[0]:""}geraetOrder(e){return this.geraete?this.geraete.findIndex(t=>t.id===e):0}geraetText(e){if(!this.geraete)return"";const t=this.geraete.filter(t=>t.id===e).map(e=>e.name);return 1===t.length?t[0]:""}getTitle(e){return e.programm+" - "+this.geraetText(e.geraet)}itemTapped(e,t){}}return e.\u0275fac=function(t){return new(t||e)(a.Y36(l.v))},e.\u0275cmp=a.Xpm({type:e,selectors:[["app-last-top-results"]],decls:18,vars:3,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",3,"click",4,"ngFor","ngForOf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],["size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",1,"align-self-start",3,"click"],[3,"item","title"]],template:function(e,t){1&e&&(a.TgZ(0,"ion-header"),a.TgZ(1,"ion-toolbar"),a.TgZ(2,"ion-buttons",0),a.TgZ(3,"ion-menu-toggle"),a.TgZ(4,"ion-button"),a._UZ(5,"ion-icon",1),a.qZA(),a.qZA(),a.qZA(),a.TgZ(6,"ion-title"),a.TgZ(7,"ion-grid",2),a.TgZ(8,"ion-row"),a.TgZ(9,"ion-col"),a.TgZ(10,"ion-label"),a._uU(11,"Top Resultate"),a.qZA(),a.qZA(),a.YNc(12,d,5,1,"ion-col",3),a.qZA(),a.qZA(),a.qZA(),a.YNc(13,m,2,1,"ion-note",4),a.qZA(),a.qZA(),a.TgZ(14,"ion-content"),a.TgZ(15,"ion-grid",2),a.TgZ(16,"ion-row"),a.YNc(17,h,2,2,"ion-col",5),a.qZA(),a.qZA(),a.qZA()),2&e&&(a.xp6(12),a.Q6J("ngIf",!t.competition),a.xp6(1),a.Q6J("ngIf",t.competition),a.xp6(4),a.Q6J("ngForOf",t.items))},directives:[c.Gu,c.sr,c.Sm,c.zc,c.YG,c.gu,c.wd,c.jY,c.Nd,c.wI,c.Q$,o.O5,c.W2,o.sg,c.Ie,c.t9,c.QI,n.JJ,n.On,c.n0,c.uN,p.G],pipes:[o.uU],styles:[""]}),e})();var Z=i(5051);const T=[{path:"",component:f}];let b=(()=>{class e{}return e.\u0275fac=function(t){return new(t||e)},e.\u0275mod=a.oAB({type:e}),e.\u0275inj=a.cJS({imports:[[o.ez,n.u5,c.Pc,s.Bz.forChild(T),Z.K]]}),e})()}}]);