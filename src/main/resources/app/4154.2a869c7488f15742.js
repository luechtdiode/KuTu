"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[4154],{4154:(A,p,c)=>{c.r(p),c.d(p,{LastTopResultsPageModule:()=>y});var l=c(6814),g=c(95),d=c(335),s=c(3582),f=c(2181),m=c(2565),t=c(2029),h=c(9253);function _(o,r){if(1&o&&(t.TgZ(0,"ion-select-option",9),t._uU(1),t.ALo(2,"date"),t.qZA()),2&o){const e=r.$implicit;t.Q6J("value",e.uuid),t.xp6(1),t.hij(" ",e.titel+" "+t.xi3(2,2,e.datum,"dd-MM-yy"),"")}}function T(o,r){if(1&o){const e=t.EpF();t.TgZ(0,"ion-select",7),t.NdJ("ngModelChange",function(n){t.CHM(e);const a=t.oxw(2);return t.KtG(a.competition=n)}),t.YNc(1,_,3,5,"ion-select-option",8),t.qZA()}if(2&o){const e=t.oxw(2);t.Q6J("ngModel",e.competition),t.xp6(1),t.Q6J("ngForOf",e.getCompetitions())}}function x(o,r){if(1&o&&(t.TgZ(0,"ion-col")(1,"ion-item")(2,"ion-label"),t._uU(3,"Wettkampf"),t.qZA(),t.YNc(4,T,2,2,"ion-select",6),t.qZA()()),2&o){const e=t.oxw();t.xp6(4),t.Q6J("ngIf",e.getCompetitions().length>0)}}function v(o,r){if(1&o&&(t.TgZ(0,"ion-note",10),t._uU(1),t.qZA()),2&o){const e=t.oxw();t.xp6(1),t.hij(" ",e.competitionName()," ")}}function Z(o,r){if(1&o&&(t.TgZ(0,"ion-col",13),t._UZ(1,"result-display",14),t.qZA()),2&o){const e=r.$implicit,i=t.oxw(3);t.uIk("size-xl",i.getMaxColumnSpec()),t.xp6(1),t.Q6J("item",e)("title",i.getTitle(e))("groupedBy",i.groupBy.PROGRAMM)}}function b(o,r){if(1&o&&(t.TgZ(0,"ion-row"),t.YNc(1,Z,2,4,"ion-col",12),t.qZA()),2&o){const e=r.$implicit,i=t.oxw(2);t.xp6(1),t.Q6J("ngForOf",i.getWertungen(e))}}function w(o,r){if(1&o&&(t.TgZ(0,"ion-grid",2),t.YNc(1,b,2,1,"ion-row",11),t.qZA()),2&o){const e=t.oxw();t.xp6(1),t.Q6J("ngForOf",e.getProgramme())}}function S(o,r){if(1&o&&(t.TgZ(0,"ion-col",16),t._UZ(1,"result-display",14),t.qZA()),2&o){const e=r.$implicit,i=t.oxw(3);t.uIk("size-xl",i.getMaxColumnSpec()),t.xp6(1),t.Q6J("item",e)("title",i.getTitle(e))("groupedBy",i.groupBy.PROGRAMM)}}function z(o,r){if(1&o&&(t.TgZ(0,"ion-row"),t.YNc(1,S,2,4,"ion-col",15),t.qZA()),2&o){const e=r.$implicit,i=t.oxw(2);t.xp6(1),t.Q6J("ngForOf",i.getWertungen(e))}}function k(o,r){if(1&o&&(t.TgZ(0,"ion-grid",2),t.YNc(1,z,2,1,"ion-row",11),t.qZA()),2&o){const e=t.oxw();t.xp6(1),t.Q6J("ngForOf",e.getProgramme())}}let M=(()=>{class o{backendService;groupBy=m.X;items=[];toptop={};geraete=[];ngOnInit(){this.backendService.competitionSubject.subscribe(e=>{this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}),this.backendService.newLastResults.pipe((0,f.h)(i=>!!i&&!!i.lastTopResults)).subscribe(i=>{this.items=[],this.toptop={},Object.keys(i.lastTopResults).forEach(n=>{const a=i.lastTopResults[n],u=this.toptop[a.wertung.wettkampfdisziplinId];u?u.wertung.endnote<a.wertung.endnote&&(this.toptop[a.wertung.wettkampfdisziplinId]=a):this.toptop[a.wertung.wettkampfdisziplinId]=a}),Object.keys(this.toptop).forEach(n=>{this.items.push(this.toptop[n])}),this.sortItems()})})}constructor(e){this.backendService=e,this.backendService.competitions||this.backendService.getCompetitions()}sortItems(){this.items=this.items.sort((e,i)=>{let n=e.programm.localeCompare(i.programm);return 0===n&&(n=this.geraetOrder(e.geraet)-this.geraetOrder(i.geraet)),n})}get stationFreezed(){return this.backendService.stationFreezed}set competition(e){this.stationFreezed||(this.backendService.getDurchgaenge(e),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";const e=this.backendService.competitions.filter(i=>i.uuid===this.backendService.competition).map(i=>i.titel+", am "+(i.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===e.length?e[0]:""}geraetOrder(e){return this.geraete?this.geraete.findIndex(n=>n.id===e):0}geraetText(e){if(!this.geraete)return"";const i=this.geraete.filter(n=>n.id===e).map(n=>n.name);return 1===i.length?i[0]:""}getColumnSpec(){return this.geraete?.length||0}getMaxColumnSpec(){return Math.min(12,Math.max(1,Math.floor(12/this.geraete.length+.5)))}getTitle(e){return e.programm+" - "+this.geraetText(e.geraet)}onlyUnique(e,i,n){return n.indexOf(e)===i}getProgramme(){return this.items.map(e=>e.programm).filter(this.onlyUnique)}getWertungen(e){return this.items.filter(i=>i.programm===e)}itemTapped(e,i){}static \u0275fac=function(i){return new(i||o)(t.Y36(h.v))};static \u0275cmp=t.Xpm({type:o,selectors:[["app-last-top-results"]],decls:17,vars:4,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["no-padding","",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","3",1,"align-self-start"],[3,"item","title","groupedBy"],["class","align-self-start","size-xs","12","size-sm","6","size-md","4","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-sm","6","size-md","4","size-lg","3",1,"align-self-start"]],template:function(i,n){1&i&&(t.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),t._UZ(5,"ion-icon",1),t.qZA()()(),t.TgZ(6,"ion-title")(7,"ion-grid",2)(8,"ion-row")(9,"ion-col")(10,"ion-label"),t._uU(11,"Top Resultate"),t.qZA()(),t.YNc(12,x,5,1,"ion-col",3),t.qZA()()(),t.YNc(13,v,2,1,"ion-note",4),t.qZA()(),t.TgZ(14,"ion-content"),t.YNc(15,w,2,1,"ion-grid",5),t.YNc(16,k,2,1,"ion-grid",5),t.qZA()),2&i&&(t.xp6(12),t.Q6J("ngIf",!n.competition),t.xp6(1),t.Q6J("ngIf",n.competition),t.xp6(2),t.Q6J("ngIf",n.items.length>0&&6===n.getColumnSpec()),t.xp6(1),t.Q6J("ngIf",n.items.length>0&&6!==n.getColumnSpec()))},dependencies:[l.sg,l.O5,g.JJ,g.On,s.YG,s.Sm,s.wI,s.W2,s.jY,s.Gu,s.gu,s.Ie,s.Q$,s.zc,s.uN,s.Nd,s.t9,s.n0,s.wd,s.sr,s.QI,m.G,l.uU]})}return o})();var I=c(3573);const R=[{path:"",component:M}];let y=(()=>{class o{static \u0275fac=function(i){return new(i||o)};static \u0275mod=t.oAB({type:o});static \u0275inj=t.cJS({imports:[l.ez,g.u5,s.Pc,d.Bz.forChild(R),I.K]})}return o})()}}]);