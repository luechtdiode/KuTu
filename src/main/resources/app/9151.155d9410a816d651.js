"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[9151],{9151:(M,g,u)=>{u.r(g),u.d(g,{WertungEditorPageModule:()=>x});var c=u(6895),l=u(433),h=u(5472),s=u(502),m=u(5861);const p=(0,u(7423).fo)("Keyboard");var e=u(8274),f=u(600);const w=["wertungsform"],v=["enote"],I=["dnote"];function k(r,d){1&r&&(e.TgZ(0,"ion-item"),e._uU(1," Ung\xfcltige Eingabe. Die Werte m\xfcssen im Format ##.## (2-3 Nachkommastellen mit Dezimalpunkt) eingegeben werden. "),e.qZA())}function Z(r,d){if(1&r&&(e.TgZ(0,"ion-button",17,18),e._UZ(2,"ion-icon",19),e._uU(3,"Speichern & Weiter"),e.qZA()),2&r){const t=e.oxw(),n=e.MAs(13);e.Q6J("disabled",t.waiting||!n.valid)}}function U(r,d){if(1&r){const t=e.EpF();e.TgZ(0,"ion-button",20),e.NdJ("click",function(){e.CHM(t);const i=e.oxw(),a=e.MAs(13);return e.KtG(i.saveClose(a))}),e._UZ(1,"ion-icon",21),e._uU(2,"Speichern"),e.qZA()}if(2&r){const t=e.oxw(),n=e.MAs(13);e.Q6J("disabled",t.waiting||!n.valid)}}const y=[{path:"",component:(()=>{class r{constructor(t,n,i,a,o,E,C){this.navCtrl=t,this.route=n,this.alertCtrl=i,this.toastController=a,this.backendService=o,this.platform=E,this.zone=C,this.waiting=!1,this.isDNoteUsed=!0,this.durchgang=o.durchgang,this.step=o.step,this.geraetId=o.geraet;const S=parseInt(this.route.snapshot.paramMap.get("itemId"));this.updateUI(o.wertungen.find(_=>_.id===S)),this.isDNoteUsed=this.item.isDNoteUsed}ionViewWillLeave(){this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0)}ionViewWillEnter(){this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0),this.subscription=this.backendService.wertungUpdated.subscribe(t=>{console.log("incoming wertung from service",t),t.wertung.athletId===this.wertung.athletId&&t.wertung.wettkampfdisziplinId===this.wertung.wettkampfdisziplinId&&t.wertung.endnote!==this.wertung.endnote&&(console.log("updateing wertung from service"),this.item.wertung=Object.assign({},t.wertung),this.itemOriginal.wertung=Object.assign({},t.wertung),this.wertung=Object.assign({noteD:0,noteE:0,endnote:0},this.item.wertung))}),this.platform.ready().then(()=>{setTimeout(()=>{p.show&&(p.show(),console.log("keyboard called")),this.isDNoteUsed&&this.dnote?(this.dnote.setFocus(),console.log("dnote focused")):this.enote&&(this.enote.setFocus(),console.log("enote focused"))},400)})}editable(){return this.backendService.loggedIn}updateUI(t){this.zone.run(()=>{this.waiting=!1,this.item=Object.assign({},t),this.itemOriginal=Object.assign({},t),this.wertung=Object.assign({noteD:0,noteE:0,endnote:0},this.itemOriginal.wertung),this.ionViewWillEnter()})}ensureInitialValues(t){return Object.assign(this.wertung,t)}saveClose(t){this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(t.value)).subscribe({next:n=>{this.updateUI(n),this.navCtrl.pop()},error:n=>{this.updateUI(this.itemOriginal),console.log(n)}})}save(t){this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(t.value)).subscribe({next:n=>{this.updateUI(n)},error:n=>{this.updateUI(this.itemOriginal),console.log(n)}})}saveNext(t){this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(t.value)).subscribe({next:n=>{this.waiting=!1;const i=this.backendService.wertungen.findIndex(o=>o.wertung.id===n.wertung.id);i<0&&console.log("unexpected wertung - id matches not with current wertung: "+n.wertung.id);let a=i+1;if(i<0)a=0;else if(i>=this.backendService.wertungen.length-1){if(0===this.backendService.wertungen.filter(o=>void 0===o.wertung.endnote).length)return this.navCtrl.pop(),void this.toastSuggestCompletnessCheck();a=this.backendService.wertungen.findIndex(o=>void 0===o.wertung.endnote),this.toastMissingResult(t,this.backendService.wertungen[a].vorname+" "+this.backendService.wertungen[a].name)}t.resetForm(),this.updateUI(this.backendService.wertungen[a])},error:n=>{this.updateUI(this.itemOriginal),console.log(n)}})}nextEmptyOrFinish(t){const n=this.backendService.wertungen.findIndex(i=>i.wertung.id===this.wertung.id);if(0===this.backendService.wertungen.filter((i,a)=>a>n&&void 0===i.wertung.endnote).length)return this.navCtrl.pop(),void this.toastSuggestCompletnessCheck();{const i=this.backendService.wertungen.findIndex((a,o)=>o>n&&void 0===a.wertung.endnote);this.toastMissingResult(t,this.backendService.wertungen[i].vorname+" "+this.backendService.wertungen[i].name),t.resetForm(),this.updateUI(this.backendService.wertungen[i])}}geraetName(){return this.backendService.geraete?this.backendService.geraete.find(t=>t.id===this.geraetId).name:""}toastSuggestCompletnessCheck(){var t=this;return(0,m.Z)(function*(){(yield t.toastController.create({header:"Qualit\xe4tskontrolle",message:"Es sind jetzt alle Resultate erfasst. Bitte ZU ZWEIT die Resultate pr\xfcfen und abschliessen!",animated:!0,position:"middle",buttons:[{text:"OK, gelesen.",icon:"checkmark-circle",role:"cancel",handler:()=>{console.log("OK, gelesen. clicked")}}]})).present()})()}toastMissingResult(t,n){var i=this;return(0,m.Z)(function*(){(yield i.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"Nach der Erfassung der letzten Wertung in dieser Riege scheint es noch leere Wertungen zu geben. Bitte pr\xfcfen, ob "+n.toUpperCase()+" geturnt hat.",buttons:[{text:"Nicht geturnt",role:"edit",handler:()=>{i.nextEmptyOrFinish(t)}},{text:"Korrigieren",role:"cancel",handler:()=>{console.log("Korrigieren clicked")}}]})).present()})()}}return r.\u0275fac=function(t){return new(t||r)(e.Y36(s.SH),e.Y36(h.gz),e.Y36(s.Br),e.Y36(s.yF),e.Y36(f.v),e.Y36(s.t4),e.Y36(e.R0b))},r.\u0275cmp=e.Xpm({type:r,selectors:[["app-wertung-editor"]],viewQuery:function(t,n){if(1&t&&(e.Gf(w,5),e.Gf(v,5),e.Gf(I,5)),2&t){let i;e.iGM(i=e.CRH())&&(n.form=i.first),e.iGM(i=e.CRH())&&(n.enote=i.first),e.iGM(i=e.CRH())&&(n.dnote=i.first)}},decls:34,vars:14,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[1,"riege"],[3,"ngSubmit","keyup.enter"],["wertungsform","ngForm"],[3,"hidden"],["autofocus","","placeholder","Notenwert im Format ##.## (2-3 Nachkommastellen mit Dezimalpunkt)","type","number","name","noteD","step","0.01","min","0","max","100",3,"readonly","disabled","ngModel"],["dnote",""],["autofocus","","placeholder","Notenwert im Format ##.## (2-3 Nachkommastellen mit Dezimalpunkt)","type","number","name","noteE","step","0.01","min","0","max","100","required","",3,"readonly","disabled","ngModel"],["enote",""],["type","number","readonly","","name","endnote",3,"ngModel"],["endnote",""],[4,"ngIf"],["size","large","expand","block","type","submit","color","success",3,"disabled",4,"ngIf"],["size","large","expand","block","color","secondary",3,"disabled","click",4,"ngIf"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name","arrow-forward-circle-outline"],["size","large","expand","block","color","secondary",3,"disabled","click"],["slot","start","name","checkmark-circle"]],template:function(t,n){if(1&t){const i=e.EpF();e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title"),e._uU(5),e.qZA(),e.TgZ(6,"ion-note",2)(7,"div",3),e._uU(8),e.qZA(),e.TgZ(9,"div",4),e._uU(10),e.qZA()()()(),e.TgZ(11,"ion-content")(12,"form",5,6),e.NdJ("ngSubmit",function(){e.CHM(i);const o=e.MAs(13);return e.KtG(n.saveNext(o))})("keyup.enter",function(){e.CHM(i);const o=e.MAs(13);return e.KtG(n.saveNext(o))}),e.TgZ(14,"ion-list")(15,"ion-item",7)(16,"ion-label"),e._uU(17,"D-Note"),e.qZA(),e._UZ(18,"ion-input",8,9),e.qZA(),e.TgZ(20,"ion-item")(21,"ion-label"),e._uU(22,"E-Note"),e.qZA(),e._UZ(23,"ion-input",10,11),e.qZA(),e.TgZ(25,"ion-item")(26,"ion-label"),e._uU(27,"Endnote"),e.qZA(),e._UZ(28,"ion-input",12,13),e.qZA()(),e.TgZ(30,"ion-list"),e.YNc(31,k,2,0,"ion-item",14),e.YNc(32,Z,4,1,"ion-button",15),e.YNc(33,U,3,1,"ion-button",16),e.qZA()()()}if(2&t){const i=e.MAs(13);e.xp6(5),e.Oqu(n.geraetName()),e.xp6(3),e.Oqu(n.item.vorname+" "+n.item.name),e.xp6(2),e.Oqu(n.wertung.riege),e.xp6(5),e.Q6J("hidden",!n.isDNoteUsed),e.xp6(3),e.Q6J("readonly",!n.editable())("disabled",n.waiting)("ngModel",n.wertung.noteD),e.xp6(5),e.Q6J("readonly",!n.editable())("disabled",n.waiting)("ngModel",n.wertung.noteE),e.xp6(5),e.Q6J("ngModel",n.wertung.endnote),e.xp6(3),e.Q6J("ngIf",!i.valid),e.xp6(1),e.Q6J("ngIf",n.editable()),e.xp6(1),e.Q6J("ngIf",n.editable())}},dependencies:[c.O5,l._Y,l.JJ,l.JL,l.Q7,l.On,l.F,s.oU,s.YG,s.Sm,s.W2,s.Gu,s.gu,s.pK,s.Ie,s.Q$,s.q_,s.uN,s.wd,s.sr,s.as,s.cs],styles:[".riege[_ngcontent-%COMP%]{font-size:small;padding-right:16px;color:var(--ion-color-medium)}.athlet[_ngcontent-%COMP%]{font-size:larger;padding-right:16px;font-weight:bolder;color:var(--ion-color-primary)}"]}),r})()}];let x=(()=>{class r{}return r.\u0275fac=function(t){return new(t||r)},r.\u0275mod=e.oAB({type:r}),r.\u0275inj=e.cJS({imports:[c.ez,l.u5,s.Pc,h.Bz.forChild(y)]}),r})()}}]);