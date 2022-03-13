"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[5613],{5613:(O,c,u)=>{u.r(c),u.d(c,{WertungEditorPageModule:()=>x});var h=u(9808),d=u(2382),p=u(1631),s=u(7674),m=u(655),e=u(7587),f=u(2313),b=u(4302),w=u(600);const v=["wertungsform"],I=["enote"],k=["dnote"];function y(r,l){if(1&r&&(e.TgZ(0,"ion-button",16,17),e._UZ(2,"ion-icon",18),e._uU(3,"Speichern & Weiter"),e.qZA()),2&r){const t=e.oxw(),n=e.MAs(13);e.Q6J("disabled",t.waiting||!n.valid)}}function U(r,l){if(1&r){const t=e.EpF();e.TgZ(0,"ion-button",19),e.NdJ("click",function(){e.CHM(t);const i=e.oxw(),o=e.MAs(13);return i.saveClose(o)}),e._UZ(1,"ion-icon",20),e._uU(2,"Speichern"),e.qZA()}if(2&r){const t=e.oxw(),n=e.MAs(13);e.Q6J("disabled",t.waiting||!n.valid)}}const Z=[{path:"",component:(()=>{class r{constructor(t,n,i,o,a,E,g,S,C){this.navCtrl=t,this.route=n,this.sanitizer=i,this.alertCtrl=o,this.toastController=a,this.keyboard=E,this.backendService=g,this.platform=S,this.zone=C,this.waiting=!1,this.isDNoteUsed=!0,this.durchgang=g.durchgang,this.step=g.step,this.geraetId=g.geraet;const M=parseInt(this.route.snapshot.paramMap.get("itemId"));this.updateUI(g.wertungen.find(W=>W.id===M)),this.isDNoteUsed=this.item.isDNoteUsed}ionViewWillLeave(){this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0)}ionViewWillEnter(){this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0),this.subscription=this.backendService.wertungUpdated.subscribe(t=>{console.log("incoming wertung from service",t),t.wertung.athletId===this.wertung.athletId&&t.wertung.wettkampfdisziplinId===this.wertung.wettkampfdisziplinId&&t.wertung.endnote!==this.wertung.endnote&&(console.log("updateing wertung from service"),this.item.wertung=Object.assign({},t.wertung),this.itemOriginal.wertung=Object.assign({},t.wertung),this.wertung=Object.assign({noteD:0,noteE:0,endnote:0},this.item.wertung))}),this.platform.ready().then(()=>{setTimeout(()=>{this.keyboard.show&&(this.keyboard.show(),console.log("keyboard called")),this.isDNoteUsed&&this.dnote?(this.dnote.setFocus(),console.log("dnote focused")):this.enote&&(this.enote.setFocus(),console.log("enote focused"))},400)})}editable(){return this.backendService.loggedIn}updateUI(t){this.zone.run(()=>{this.waiting=!1,this.item=Object.assign({},t),this.itemOriginal=Object.assign({},t),this.wertung=Object.assign({noteD:0,noteE:0,endnote:0},this.itemOriginal.wertung),this.ionViewWillEnter()})}ensureInitialValues(t){return Object.assign(this.wertung,t)}saveClose(t){this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(t.value)).subscribe({next:n=>{this.updateUI(n),this.navCtrl.pop()},error:n=>{this.updateUI(this.itemOriginal),console.log(n)}})}save(t){this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(t.value)).subscribe({next:n=>{this.updateUI(n)},error:n=>{this.updateUI(this.itemOriginal),console.log(n)}})}saveNext(t){this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(t.value)).subscribe({next:n=>{this.waiting=!1;const i=this.backendService.wertungen.findIndex(a=>a.wertung.id===n.wertung.id);i<0&&console.log("unexpected wertung - id matches not with current wertung: "+n.wertung.id);let o=i+1;if(i<0)o=0;else if(i>=this.backendService.wertungen.length-1){if(0===this.backendService.wertungen.filter(a=>void 0===a.wertung.endnote).length)return this.navCtrl.pop(),void this.toastSuggestCompletnessCheck();o=this.backendService.wertungen.findIndex(a=>void 0===a.wertung.endnote),this.toastMissingResult(t,this.backendService.wertungen[o].vorname+" "+this.backendService.wertungen[o].name)}t.resetForm(),this.updateUI(this.backendService.wertungen[o])},error:n=>{this.updateUI(this.itemOriginal),console.log(n)}})}nextEmptyOrFinish(t){const n=this.backendService.wertungen.findIndex(i=>i.wertung.id===this.wertung.id);if(0===this.backendService.wertungen.filter((i,o)=>o>n&&void 0===i.wertung.endnote).length)return this.navCtrl.pop(),void this.toastSuggestCompletnessCheck();{const i=this.backendService.wertungen.findIndex((o,a)=>a>n&&void 0===o.wertung.endnote);this.toastMissingResult(t,this.backendService.wertungen[i].vorname+" "+this.backendService.wertungen[i].name),t.resetForm(),this.updateUI(this.backendService.wertungen[i])}}geraetName(){return this.backendService.geraete?this.backendService.geraete.find(t=>t.id===this.geraetId).name:""}toastSuggestCompletnessCheck(){return(0,m.mG)(this,void 0,void 0,function*(){(yield this.toastController.create({header:"Qualit\xe4tskontrolle",message:"Es sind jetzt alle Resultate erfasst. Bitte ZU ZWEIT die Resultate pr\xfcfen und abschliessen!",animated:!0,position:"middle",buttons:[{text:"OK, gelesen.",icon:"checkmark-circle",role:"cancel",handler:()=>{console.log("OK, gelesen. clicked")}}]})).present()})}toastMissingResult(t,n){return(0,m.mG)(this,void 0,void 0,function*(){(yield this.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"Nach der Erfassung der letzten Wertung in dieser Riege scheint es noch leere Wertungen zu geben. Bitte pr\xfcfen, ob "+n.toUpperCase()+" geturnt hat.",buttons:[{text:"Nicht geturnt",role:"edit",handler:()=>{this.nextEmptyOrFinish(t)}},{text:"Korrigieren",role:"cancel",handler:()=>{console.log("Korrigieren clicked")}}]})).present()})}}return r.\u0275fac=function(t){return new(t||r)(e.Y36(s.SH),e.Y36(p.gz),e.Y36(f.H7),e.Y36(s.Br),e.Y36(s.yF),e.Y36(b.N1),e.Y36(w.v),e.Y36(s.t4),e.Y36(e.R0b))},r.\u0275cmp=e.Xpm({type:r,selectors:[["app-wertung-editor"]],viewQuery:function(t,n){if(1&t&&(e.Gf(v,5),e.Gf(I,5),e.Gf(k,5)),2&t){let i;e.iGM(i=e.CRH())&&(n.form=i.first),e.iGM(i=e.CRH())&&(n.enote=i.first),e.iGM(i=e.CRH())&&(n.dnote=i.first)}},decls:33,vars:13,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[1,"riege"],[3,"ngSubmit","keyup.enter"],["wertungsform","ngForm"],[3,"hidden"],["autofocus","","type","number","name","noteD",3,"readonly","disabled","ngModel"],["dnote",""],["autofocus","","type","number","name","noteE","step","0.01","min","0","max","100","required","",3,"readonly","disabled","ngModel"],["enote",""],["readonly","","name","endnote",3,"ngModel"],["endnote",""],["size","large","expand","block","type","submit","color","success",3,"disabled",4,"ngIf"],["size","large","expand","block","color","secondary",3,"disabled","click",4,"ngIf"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name","arrow-dropright-circle"],["size","large","expand","block","color","secondary",3,"disabled","click"],["slot","start","name","checkmark-circle"]],template:function(t,n){if(1&t){const i=e.EpF();e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title"),e._uU(5),e.qZA(),e.TgZ(6,"ion-note",2)(7,"div",3),e._uU(8),e.qZA(),e.TgZ(9,"div",4),e._uU(10),e.qZA()()()(),e.TgZ(11,"ion-content")(12,"form",5,6),e.NdJ("ngSubmit",function(){e.CHM(i);const a=e.MAs(13);return n.saveNext(a)})("keyup.enter",function(){e.CHM(i);const a=e.MAs(13);return n.saveNext(a)}),e.TgZ(14,"ion-list")(15,"ion-item",7)(16,"ion-label"),e._uU(17,"D-Note"),e.qZA(),e._UZ(18,"ion-input",8,9),e.qZA(),e.TgZ(20,"ion-item")(21,"ion-label"),e._uU(22,"E-Note"),e.qZA(),e._UZ(23,"ion-input",10,11),e.qZA(),e.TgZ(25,"ion-item")(26,"ion-label"),e._uU(27,"Endnote"),e.qZA(),e._UZ(28,"ion-input",12,13),e.qZA()(),e.TgZ(30,"ion-list"),e.YNc(31,y,4,1,"ion-button",14),e.YNc(32,U,3,1,"ion-button",15),e.qZA()()()}2&t&&(e.xp6(5),e.Oqu(n.geraetName()),e.xp6(3),e.Oqu(n.item.vorname+" "+n.item.name),e.xp6(2),e.Oqu(n.wertung.riege),e.xp6(5),e.Q6J("hidden",!n.isDNoteUsed),e.xp6(3),e.Q6J("readonly",!n.editable())("disabled",n.waiting)("ngModel",n.wertung.noteD),e.xp6(5),e.Q6J("readonly",!n.editable())("disabled",n.waiting)("ngModel",n.wertung.noteE),e.xp6(5),e.Q6J("ngModel",n.wertung.endnote),e.xp6(3),e.Q6J("ngIf",n.editable()),e.xp6(1),e.Q6J("ngIf",n.editable()))},directives:[s.Gu,s.sr,s.Sm,s.oU,s.cs,s.wd,s.uN,s.W2,d._Y,d.JL,d.F,s.q_,s.Ie,s.Q$,s.pK,s.as,d.JJ,d.On,d.Q7,s.j9,h.O5,s.YG,s.gu],styles:[".riege[_ngcontent-%COMP%]{font-size:small;padding-right:16px;color:var(--ion-color-medium)}.athlet[_ngcontent-%COMP%]{font-size:larger;padding-right:16px;font-weight:bolder;color:var(--ion-color-primary)}"]}),r})()}];let x=(()=>{class r{}return r.\u0275fac=function(t){return new(t||r)},r.\u0275mod=e.oAB({type:r}),r.\u0275inj=e.cJS({providers:[b.N1],imports:[[h.ez,d.u5,s.Pc,p.Bz.forChild(Z)]]}),r})()}}]);