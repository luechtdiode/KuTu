"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[165],{165:(I,g,c)=>{c.r(g),c.d(g,{LastResultsPageModule:()=>C});var r=c(9808),a=c(2382),m=c(1631),s=c(3349),d=c(9300),e=c(7587),p=c(600),h=c(2997);function f(n,l){if(1&n&&(e.TgZ(0,"ion-select-option",9),e._uU(1),e.ALo(2,"date"),e.qZA()),2&n){const t=l.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function Z(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",7),e.NdJ("ngModelChange",function(o){return e.CHM(t),e.oxw(2).competition=o}),e.YNc(1,f,3,5,"ion-select-option",8),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function T(n,l){if(1&n&&(e.TgZ(0,"ion-col"),e.TgZ(1,"ion-item"),e.TgZ(2,"ion-label"),e._uU(3,"Wettkampf"),e.qZA(),e.YNc(4,Z,2,2,"ion-select",6),e.qZA(),e.qZA()),2&n){const t=e.oxw();e.xp6(4),e.Q6J("ngIf",t.getCompetitions().length>0)}}function x(n,l){if(1&n&&(e.TgZ(0,"ion-note",10),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij(" ",t.competitionName()," ")}}const v=function(n){return{"gradient-border":n}};function _(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-col",11),e.NdJ("click",function(o){const S=e.CHM(t).$implicit;return e.oxw().itemTapped(o,S)}),e.TgZ(1,"div",12),e._UZ(2,"result-display",13),e.qZA(),e.qZA()}if(2&n){const t=l.$implicit,i=e.oxw();e.xp6(1),e.Q6J("ngClass",e.VKq(3,v,i.isNew(t))),e.xp6(1),e.Q6J("item",t)("title",i.getTitle(t))}}let A=(()=>{class n{constructor(t,i){this.navCtrl=t,this.backendService=i,this.items=[],this.geraete=[],this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(t=>{this.geraete=t||[],this.sortItems()}),this.backendService.newLastResults.pipe((0,d.h)(t=>!!t&&!!t.results)).subscribe(t=>{this.lastItems=this.items.map(i=>i.id*this.geraete.length+i.geraet),this.items=[],Object.keys(t.results).forEach(i=>{this.items.push(t.results[i])}),this.sortItems()})}sortItems(){this.items=this.items.sort((t,i)=>{let o=t.programm.localeCompare(i.programm);return 0===o&&(o=this.geraetOrder(t.geraet)-this.geraetOrder(i.geraet)),o}).filter(t=>void 0!==t.wertung.endnote)}isNew(t){return 0===this.lastItems.filter(i=>i===t.id*this.geraete.length+t.geraet).length}get stationFreezed(){return this.backendService.stationFreezed}set competition(t){this.stationFreezed||(this.backendService.getDurchgaenge(t),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";const t=this.backendService.competitions.filter(i=>i.uuid===this.backendService.competition).map(i=>i.titel+", am "+(i.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===t.length?t[0]:""}geraetOrder(t){return this.geraete?this.geraete.findIndex(o=>o.id===t):0}geraetText(t){if(!this.geraete)return"";const i=this.geraete.filter(o=>o.id===t).map(o=>o.name);return 1===i.length?i[0]:""}getTitle(t){return t.programm+" - "+this.geraetText(t.geraet)}itemTapped(t,i){}}return n.\u0275fac=function(t){return new(t||n)(e.Y36(s.SH),e.Y36(p.v))},n.\u0275cmp=e.Xpm({type:n,selectors:[["app-last-results"]],decls:18,vars:3,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",3,"click",4,"ngFor","ngForOf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],["size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",1,"align-self-start",3,"click"],[3,"ngClass"],[3,"item","title"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header"),e.TgZ(1,"ion-toolbar"),e.TgZ(2,"ion-buttons",0),e.TgZ(3,"ion-menu-toggle"),e.TgZ(4,"ion-button"),e._UZ(5,"ion-icon",1),e.qZA(),e.qZA(),e.qZA(),e.TgZ(6,"ion-title"),e.TgZ(7,"ion-grid",2),e.TgZ(8,"ion-row"),e.TgZ(9,"ion-col"),e.TgZ(10,"ion-label"),e._uU(11,"Aktuelle Resultate"),e.qZA(),e.qZA(),e.YNc(12,T,5,1,"ion-col",3),e.qZA(),e.qZA(),e.qZA(),e.YNc(13,x,2,1,"ion-note",4),e.qZA(),e.qZA(),e.TgZ(14,"ion-content"),e.TgZ(15,"ion-grid",2),e.TgZ(16,"ion-row"),e.YNc(17,_,3,5,"ion-col",5),e.qZA(),e.qZA(),e.qZA()),2&t&&(e.xp6(12),e.Q6J("ngIf",!i.competition),e.xp6(1),e.Q6J("ngIf",i.competition),e.xp6(4),e.Q6J("ngForOf",i.items))},directives:[s.Gu,s.sr,s.Sm,s.zc,s.YG,s.gu,s.wd,s.jY,s.Nd,s.wI,s.Q$,r.O5,s.W2,r.sg,s.Ie,s.t9,s.QI,a.JJ,a.On,s.n0,s.uN,r.mk,h.G],pipes:[r.uU],styles:[""]}),n})();var b=c(5051);const k=[{path:"",component:A}];let C=(()=>{class n{}return n.\u0275fac=function(t){return new(t||n)},n.\u0275mod=e.oAB({type:n}),n.\u0275inj=e.cJS({imports:[[r.ez,a.u5,s.Pc,m.Bz.forChild(k),b.K]]}),n})()}}]);